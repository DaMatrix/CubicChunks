package io.github.opencubicchunks.cubicchunks.core.falling;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.impl.EntryLocation3D;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;
import io.netty.buffer.Unpooled;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author DaPorkchop_
 */
public class LevelDBRegionProvider implements IRegionProvider<EntryLocation3D> {
    protected final DB db;

    public LevelDBRegionProvider(Path path) throws IOException {
        this.db = Iq80DBFactory.factory.open(path.toFile(), new Options()
                .writeBufferSize(67108864)
                .blockSize(16777216)
                .compressionType(CompressionType.ZLIB_RAW)
                .verifyChecksums(false));
    }

    @Override
    public void forRegion(EntryLocation3D key, CheckedConsumer<? super IRegion<EntryLocation3D>, IOException> consumer) throws IOException {
        consumer.accept(new Region(key.getRegionKey().getName()));
    }

    @Override
    public <R> Optional<R> fromExistingRegion(EntryLocation3D key, CheckedFunction<? super IRegion<EntryLocation3D>, R, IOException> func) throws IOException {
        return Optional.of(func.apply(new Region(key.getRegionKey().getName())));
    }

    @Override
    public <R> R fromRegion(EntryLocation3D key, CheckedFunction<? super IRegion<EntryLocation3D>, R, IOException> func) throws IOException {
        return func.apply(new Region(key.getRegionKey().getName()));
    }

    @Override
    public void forExistingRegion(EntryLocation3D key, CheckedConsumer<? super IRegion<EntryLocation3D>, IOException> consumer) throws IOException {
        consumer.accept(new Region(key.getRegionKey().getName()));
    }

    @Override
    public IRegion<EntryLocation3D> getRegion(EntryLocation3D key) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<IRegion<EntryLocation3D>> getExistingRegion(EntryLocation3D key) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forAllRegions(CheckedConsumer<? super IRegion<EntryLocation3D>, IOException> consumer) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {
        this.db.close();
    }

    protected class Region implements IRegion<EntryLocation3D>  {
        protected final String key;

        public Region(String key)   {
            this.key = key;
        }

        public byte[] encode(EntryLocation3D key)   {
            return Unpooled.wrappedBuffer(new byte[12]).clear() //y first
                    .writeInt(key.getEntryY())
                    .writeInt(key.getEntryX())
                    .writeInt(key.getEntryZ())
                    .array();
        }

        @Override
        public void writeValue(EntryLocation3D key, ByteBuffer value) throws IOException {
            if (value.isDirect())  {
                throw new IllegalArgumentException();
            }
            byte[] arr = value.array();
            if (value.position() != 0 || value.limit() != arr.length)   {
                arr = Arrays.copyOfRange(arr, value.position(), value.limit());
            }
            db.put(this.encode(key), arr);
        }

        @Override
        public void writeSpecial(EntryLocation3D key, Object marker) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<ByteBuffer> readValue(EntryLocation3D key) throws IOException {
            byte[] arr = db.get(this.encode(key));
            return arr == null ? Optional.empty() : Optional.of(ByteBuffer.wrap(arr));
        }

        @Override
        public boolean hasValue(EntryLocation3D key) {
            return db.get(this.encode(key)) != null;
        }

        @Override
        public void forEachKey(CheckedConsumer<? super EntryLocation3D, IOException> cons) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() throws IOException {
            //no-op
        }
    }
}
