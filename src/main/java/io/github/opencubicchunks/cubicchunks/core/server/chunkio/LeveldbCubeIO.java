/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cubicchunks.regionlib.util.Utils;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.LeveldbConfig;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import net.daporkchop.ldbjni.LevelDB;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.Logger;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class LeveldbCubeIO implements ICubeIO {
    public static LeveldbCubeIO OVERWORLD_INSTANCE;

    private static final Options COLUMN_DB_OPTIONS = new Options()
            .compressionType(LeveldbConfig.columnCompressionType)
            .verifyChecksums(false);

    private static final Options CUBE_DB_OPTIONS = new Options()
            .writeBufferSize(67108864)
            .blockSize(16777216)
            .compressionType(LeveldbConfig.cubeCompressionType)
            .verifyChecksums(false);

    private static final WriteOptions SYNC_WRITE = new WriteOptions().sync(true);

    private static final Logger LOGGER = CubicChunks.LOGGER;

    private static byte[] getColumnKey(int columnX, int columnZ) {
        return Unpooled.wrappedBuffer(new byte[8]).clear()
                .writeInt(columnX)
                .writeInt(columnZ)
                .array();
    }

    private static byte[] getCubeKey(int cubeX, int cubeY, int cubeZ) {
        return Unpooled.wrappedBuffer(new byte[12]).clear()
                .writeInt(cubeY)
                .writeInt(cubeX)
                .writeInt(cubeZ)
                .array();
    }

    static byte[] writeUncompressedNbtBytes(NBTTagCompound nbt) throws IOException {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(1 << 16);
        try {
            CompressedStreamTools.write(nbt, new ByteBufOutputStream(buf));
            byte[] arr = new byte[buf.readableBytes()];
            buf.readBytes(arr);
            return arr;
        } finally {
            buf.release();
        }
    }

    @Nonnull
    private World world;
    @Nonnull
    private ConcurrentMap<ChunkPos, NBTTagCompound> columnsToSave;
    @Nonnull
    private ConcurrentMap<CubePos, NBTTagCompound> cubesToSave;
    private final Cache<CubePos, NBTTagCompound> savedCubesCache = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .softValues()
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();
    public DB columnDb;
    public DB cubeDb;

    public LeveldbCubeIO(World world) throws IOException {
        this.world = world;

        initSave();

        // init chunk save queue
        this.columnsToSave = new ConcurrentHashMap<>();
        this.cubesToSave = new ConcurrentHashMap<>();
    }

    private synchronized DB getColumnDb() throws IOException {
        if (this.columnDb == null) {
            this.initSave();
        }
        return this.columnDb;
    }

    private synchronized DB getCubeDb() throws IOException {
        if (this.cubeDb == null) {
            this.initSave();
        }
        return this.cubeDb;
    }

    private void initSave() throws IOException {
        // TODO: make path a constructor argument
        Path path;
        if (this.world instanceof WorldServer) {
            WorldProvider prov = this.world.provider;
            if (prov.getDimension() == 0)   {
                OVERWORLD_INSTANCE = this;
            }
            path = this.world.getSaveHandler().getWorldDirectory().toPath();
            if (prov.getSaveFolder() != null) {
                path = path.resolve(prov.getSaveFolder());
            }
        } else {
            path = Paths.get(".").toAbsolutePath().resolve("clientCache").resolve("DIM" + this.world.provider.getDimension());
        }

        Utils.createDirectories(path);

        Path part2d = path.resolve("region2d");
        Utils.createDirectories(part2d);

        Path part3d = path.resolve("region3d");
        Utils.createDirectories(part3d);

        if (!LevelDB.PROVIDER.isNative())   {
            LOGGER.warn("Not using native leveldb! This can have serious performance implications.");
        }

        this.columnDb = LevelDB.PROVIDER.open(part2d.toFile(), COLUMN_DB_OPTIONS);
        this.cubeDb = LevelDB.PROVIDER.open(part3d.toFile(), CUBE_DB_OPTIONS);
    }

    @Override
    public void flush() throws IOException {
        try {
            ThreadedFileIOBase.getThreadedIOInstance().waitForFinish();
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }

        try {
            if (this.columnDb != null) {
                try {
                    this.columnDb.close();
                } finally {
                    this.columnDb = null;
                }
            }
            if (this.cubeDb != null) {
                try {
                    this.cubeDb.close();
                } finally {
                    this.cubeDb = null;
                }
            }
        } catch (IllegalStateException alreadyClosed) {
            // ignore
        } catch (Exception ex) {
            CubicChunks.LOGGER.catching(ex);
        }
    }

    @Override
    @Nullable
    public Chunk loadColumn(int chunkX, int chunkZ) throws IOException {
        DB db = this.getColumnDb();
        NBTTagCompound nbt;
        NBTTagCompound saveEntry;
        if ((saveEntry = this.columnsToSave.get(new ChunkPos(chunkX, chunkZ))) != null) {
            nbt = saveEntry;
        } else {
            // IOException makes using Optional impossible :(
            byte[] arr = db.get(getColumnKey(chunkX, chunkZ));
            if (arr == null) {
                return null;
            }
            nbt = FMLCommonHandler.instance().getDataFixer().process(FixTypes.CHUNK,
                    CompressedStreamTools.read(new ByteBufInputStream(Unpooled.wrappedBuffer(arr)), NBTSizeTracker.INFINITE));
        }
        return IONbtReader.readColumn(this.world, chunkX, chunkZ, nbt);
    }

    @Override
    @Nullable
    public PartialCubeData loadCubeAsyncPart(Chunk column, int cubeY) throws IOException {
        DB db = this.getCubeDb();
        NBTTagCompound nbt;
        NBTTagCompound saveEntry;
        if ((saveEntry = this.savedCubesCache.getIfPresent(new CubePos(column.x, cubeY, column.z))) != null) {
            nbt = saveEntry;
        } else {
            // does the database have the cube?
            byte[] arr = db.get(getCubeKey(column.x, cubeY, column.z));
            if (arr == null) {
                return null;
            }
            nbt = FMLCommonHandler.instance().getDataFixer().process(FixTypes.CHUNK,
                    CompressedStreamTools.read(new ByteBufInputStream(Unpooled.wrappedBuffer(arr)), NBTSizeTracker.INFINITE));
        }

        // restore the cube - async part
        Cube cube = IONbtReader.readCubeAsyncPart(column, column.x, cubeY, column.z, nbt);
        if (cube == null) {
            return null;
        }
        return new PartialCubeData(cube, nbt);
    }

    @Override
    public void loadCubeSyncPart(PartialCubeData info) {
        IONbtReader.readCubeSyncPart(info.cube, this.world, info.nbt);
    }

    @Override
    public void saveColumn(Chunk column) {
        if (LeveldbConfig.readOnly) {
            //only set column as not dirty
            column.setModified(false);
        } else {
            // NOTE: this function blocks the world thread
            // make it as fast as possible by offloading processing to the IO thread
            // except we have to write the NBT in this thread to avoid problems
            // with concurrent access to world data structures

            // add the column to the save queue
            this.columnsToSave.put(column.getPos(), IONbtWriter.write(column));
            column.setModified(false);

            // signal the IO thread to process the save queue
            ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
        }
    }

    @Override
    public void saveCube(Cube cube) {
        if (LeveldbConfig.readOnly) {
            //only set cube as not dirty
            cube.markSaved();
        } else {
            // NOTE: this function blocks the world thread, so make it fast

            NBTTagCompound compound = IONbtWriter.write(cube);
            cube.markSaved();
            this.cubesToSave.put(cube.getCoords(), compound);
            this.savedCubesCache.put(cube.getCoords(), compound);

            // signal the IO thread to process the save queue
            ThreadedFileIOBase.getThreadedIOInstance().queueIO(this);
        }
    }

    @Override
    public boolean columnExists(int columnX, int columnZ) {
        try {
            //this is needlessly inefficient
            return this.getColumnDb().get(getColumnKey(columnX, columnZ)) != null;
        } catch (IOException e) {
            CubicChunks.LOGGER.catching(e);
            return false;
        }
    }

    @Override
    public boolean cubeExists(int cubeX, int cubeY, int cubeZ) {
        try {
            //this is needlessly inefficient
            return this.getCubeDb().get(getCubeKey(cubeX, cubeY, cubeZ)) != null;
        } catch (IOException e) {
            CubicChunks.LOGGER.catching(e);
            return false;
        }
    }

    @Override
    public int getPendingColumnCount() {
        return columnsToSave.size();
    }

    @Override
    public int getPendingCubeCount() {
        return cubesToSave.size();
    }

    @Override
    public boolean writeNextIO() {
        try {
            // NOTE: return true to redo this call (used for batching)

            final int ColumnsBatchSize = 25;
            final int CubesBatchSize = 250;

            int numColumnsSaved = 0;
            int numCubesSaved = 0;

            //save a batch of columns
            DB columnDb = this.getColumnDb();
            try (WriteBatch batch = columnDb.createWriteBatch()) {
                Collection<Map.Entry<ChunkPos, NBTTagCompound>> included = new ArrayList<>();

                //encode columns to bytes and add them to the batch
                for (Iterator<Map.Entry<ChunkPos, NBTTagCompound>> itr = this.columnsToSave.entrySet().iterator(); itr.hasNext() && numColumnsSaved < ColumnsBatchSize; numColumnsSaved++) {
                    Map.Entry<ChunkPos, NBTTagCompound> entry = itr.next();
                    try {
                        batch.put(getColumnKey(entry.getKey().x, entry.getKey().z), writeUncompressedNbtBytes(entry.getValue()));
                        included.add(entry);
                    } catch (Throwable t) {
                        LOGGER.error(String.format("Unable to encode column (%d, %d)", entry.getKey().x, entry.getKey().z), t);
                    }
                }

                if (!included.isEmpty()) {
                    //write entire batch of columns
                    columnDb.write(batch, SYNC_WRITE);

                    //remove columns from "to save" map
                    for (Map.Entry<ChunkPos, NBTTagCompound> entry : included) {
                        this.columnsToSave.remove(entry.getKey(), entry.getValue());
                    }
                }
            }

            //save a batch of cubes
            DB cubeDb = this.getCubeDb();
            try (WriteBatch batch = cubeDb.createWriteBatch()) {
                Collection<Map.Entry<CubePos, NBTTagCompound>> included = new ArrayList<>();

                //encode cubes to bytes and add them to the batch
                for (Iterator<Map.Entry<CubePos, NBTTagCompound>> itr = this.cubesToSave.entrySet().iterator(); itr.hasNext() && numCubesSaved < CubesBatchSize; numCubesSaved++) {
                    Map.Entry<CubePos, NBTTagCompound> entry = itr.next();
                    try {
                        batch.put(getCubeKey(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()), writeUncompressedNbtBytes(entry.getValue()));
                        included.add(entry);
                    } catch (Throwable t) {
                        LOGGER.error(String.format("Unable to encode cube %d, %d, %d", entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ()), t);
                    }
                }

                if (!included.isEmpty()) {
                    //write entire batch of cubes
                    cubeDb.write(batch, SYNC_WRITE);

                    //remove cubes from "to save" map
                    for (Map.Entry<CubePos, NBTTagCompound> entry : included) {
                        this.cubesToSave.remove(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Exception occurred when saving cubes", t);
        }
        return !this.columnsToSave.isEmpty() || !this.cubesToSave.isEmpty();
    }
}
