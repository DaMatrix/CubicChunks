/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.worldgen.generator.custom;

import com.flowpowered.noise.NoiseQuality;
import com.flowpowered.noise.module.source.Perlin;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.api.worldgen.populator.CubePopulatorEvent;
import cubicchunks.api.worldgen.populator.ICubicPopulator;
import cubicchunks.util.Box;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.BasicCubeGenerator;
import cubicchunks.worldgen.generator.CubeGeneratorsRegistry;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubePrimer;
import cubicchunks.worldgen.generator.custom.biome.replacer.IBiomeBlockReplacer;
import cubicchunks.worldgen.generator.custom.builder.BiomeSource;
import cubicchunks.worldgen.generator.custom.builder.NoiseConsumer;
import cubicchunks.worldgen.generator.custom.structure.CubicCaveGenerator;
import cubicchunks.worldgen.generator.custom.structure.CubicRavineGenerator;
import cubicchunks.worldgen.generator.custom.structure.CubicStructureGenerator;
import cubicchunks.worldgen.generator.custom.structure.feature.CubicFeatureGenerator;
import cubicchunks.worldgen.generator.custom.structure.feature.CubicStrongholdGenerator;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockChorusFlower;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityShulker;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraftforge.common.MinecraftForge;
import team.pepsi.ccaddon.PorkMethods;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * A terrain generator that supports infinite(*) worlds
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomTerrainGenerator extends BasicCubeGenerator {

    private final BiomeSource biomeSource;
    private final CustomGeneratorSettings conf;
    private final ChunkGeneratorEnd endChunkGenerator;
    private Perlin groundNoise;
    private LoadingCache<Long, Integer> groundNoiseCache = CacheBuilder.<Long, Integer>newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(512)
            .build(new CacheLoader<Long, Integer>() {
                @Override
                public Integer load(Long key) {
                    int x = (int) (key >> 32);
                    int y = (int) (long) key;
                    return (int) (groundNoise.getValue(x, y, 0) * 256);
                }
            });
    private Chunk endChunkCache = null;
    //TODO: Implement more structures
    @Nonnull
    private CubicCaveGenerator caveGenerator = new CubicCaveGenerator();
    @Nonnull
    private CubicStructureGenerator ravineGenerator = new CubicRavineGenerator();
    @Nonnull
    private CubicFeatureGenerator strongholds;
    private boolean optimizationHack = false;

    public CustomTerrainGenerator(ICubicWorld world, final long seed) {
        super(world);

        String json = world.getWorldInfo().getGeneratorOptions();
        this.conf = CustomGeneratorSettings.fromJson(json);

        this.strongholds = new CubicStrongholdGenerator(conf);

        this.biomeSource = new BiomeSource(world, conf.createBiomeBlockReplacerConfig(), world.getBiomeProvider(), 2);
        initGenerator(seed);
        this.endChunkGenerator = new ChunkGeneratorEnd((World) world, true, seed, new BlockPos(0, 16000, 0));
    }

    private void initGenerator(long seed) {
        Perlin groundNoise = new Perlin();
        groundNoise.setNoiseQuality(NoiseQuality.STANDARD);
        groundNoise.setOctaveCount(6);
        groundNoise.setFrequency(0.006);
        groundNoise.setSeed((int) (seed / (Math.pow(2, 32))));
        this.groundNoise = groundNoise;
    }

    @Override
    public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        ICubePrimer primer = new CubePrimer();
        generate(primer, cubeX, cubeY, cubeZ);
        generateStructures(primer, new CubePos(cubeX, cubeY, cubeZ));
        return primer;
    }

    @Override
    public Box getPopulationRequirement(Cube cube) {
        return RECOMMENDED_POPULATOR_REQUIREMENT;
    }

    @Override
    public void recreateStructures(Cube cube) {
        this.strongholds.generate(world, null, cube.getCoords());
    }

    @Nullable
    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
        if ("Stronghold".equals(name)) {
            return strongholds.getClosestStrongholdPos((World) world, pos, true);
        }
        return null;
    }

    /**
     * Generate the cube as the specified location
     *
     * @param cubePrimer cube primer to use
     * @param cubeX      cube x location
     * @param cubeY      cube y location
     * @param cubeZ      cube z location
     */
    public void generate(final ICubePrimer cubePrimer, int cubeX, int cubeY, int cubeZ) {
        if (PorkMethods.isCubeOutOfBounds(cubeZ)) {
            IBlockState barrier = Blocks.BARRIER.getDefaultState();
            for (int x = 0; x < Cube.SIZE; x++) {
                for (int y = 0; y < Cube.SIZE; y++) {
                    for (int z = 0; z < Cube.SIZE; z++) {
                        cubePrimer.setBlockState(x, y, z, barrier);
                    }
                }
            }
            return;
        }

        if (cubeY >= 1000 && cubeY <= 1015) { //Generate End
            Chunk chunk = endChunkCache;
            if (chunk == null || chunk.x != cubeX || chunk.z != cubeZ) {
                chunk = endChunkGenerator.generateChunk(cubeX, cubeZ);
                endChunkCache = chunk;
            }
            if (!optimizationHack) {
                optimizationHack = true;
                // Recursive generation
                for (int y = 1000; y < 1016; y++) {
                    if (y == cubeY) {
                        continue;
                    }
                    world.getCubeFromCubeCoords(cubeX, y, cubeZ);
                }
                optimizationHack = false;
            }

            ExtendedBlockStorage storage = chunk.getBlockStorageArray()[cubeY - 1000];
            if (storage != null && !storage.isEmpty()) {
                for (int x = 0; x < Cube.SIZE; x++) {
                    for (int y = 0; y < Cube.SIZE; y++) {
                        for (int z = 0; z < Cube.SIZE; z++) {
                            IBlockState state = storage.get(x, y, z);
                            cubePrimer.setBlockState(x, y, z, state);
                        }
                    }
                }
            }
        }

        if (cubeY < 0) {
            IBlockState stone = Blocks.STONE.getDefaultState();
            for (int x = 0; x < Cube.SIZE; x++) {
                for (int y = 0; y < Cube.SIZE; y++) {
                    for (int z = 0; z < Cube.SIZE; z++) {
                        cubePrimer.setBlockState(x, y, z, stone);
                    }
                }
            }
            return;
        }

        BlockPos start = new BlockPos(cubeX * 4, cubeY * 2, cubeZ * 4);
        BlockPos end = start.add(4, 2, 4);
        IBlockState air = Blocks.AIR.getDefaultState();
        this.forEachScaled(start, end, new Vec3i(4, 8, 4),
                (x, y, z, dx, dy, dz, v) -> {
                    List<IBiomeBlockReplacer> replacers = biomeSource.getReplacers(x, y, z);
                    IBlockState block = Blocks.AIR.getDefaultState();
                    int size = replacers.size();
                    for (int i = 0; i < size; i++) {
                        block = replacers.get(i).getReplacedBlock(block, x, y, z, dx, dy, dz, v);
                    }

                    if (block != air) {
                        cubePrimer.setBlockState(x & 0xf, y & 0xf, z & 0xf, block);
                    }
                }
        );
    }

    public double get(int x, int y, int z) {
        double groundNoise = groundNoiseCache.getUnchecked((((long) x) << 32) | (z & 0xffffffffL));
        return MathHelper.clamp((groundNoise - y) / 255, -1, 1);
    }

    private void generateStructures(ICubePrimer cube, CubePos cubePos) {
        // generate world populator
        if (this.conf.caves) {
            this.caveGenerator.generate(world, cube, cubePos);
        }
        if (this.conf.ravines) {
            this.ravineGenerator.generate(world, cube, cubePos);
        }
        if (this.conf.strongholds) {
            this.strongholds.generate(world, cube, cubePos);
        }

    }

    /**
     * had to copypasta this from IBuilder to avoid using IBuilder because i don't want to use it, but this method is important
     */
    public void forEachScaled(Vec3i startUnscaled, Vec3i endUnscaled, Vec3i scale, NoiseConsumer consumer) {

        if (scale.getZ() != scale.getX()) {
            throw new UnsupportedOperationException("X and Z scale must be the same!");
        }
        final double/*[]*/[][] gradX = new double/*[scale.getX()]*/[scale.getY()][scale.getZ()];
        final double[]/*[]*/[] gradY = new double[scale.getX()]/*[scale.getY()]*/[scale.getZ()];
        final double[][]/*[]*/ gradZ = new double[scale.getX()][scale.getY()]/*[scale.getZ()]*/;
        final double[][][] vals = new double[scale.getX()][scale.getY()][scale.getZ()];

        int xScale = scale.getX();
        int yScale = scale.getY();
        int zScale = scale.getZ();

        double stepX = 1.0 / xScale;
        double stepY = 1.0 / yScale;
        double stepZ = 1.0 / zScale;

        int minX = startUnscaled.getX();
        int minY = startUnscaled.getY();
        int minZ = startUnscaled.getZ();
        int maxX = endUnscaled.getX();
        int maxY = endUnscaled.getY();
        int maxZ = endUnscaled.getZ();
        for (int sectionX = minX; sectionX < maxX; ++sectionX) {
            int x = sectionX * xScale;
            for (int sectionZ = minZ; sectionZ < maxZ; ++sectionZ) {
                int z = sectionZ * zScale;
                for (int sectionY = minY; sectionY < maxY; ++sectionY) {
                    int y = sectionY * yScale;

                    final double v000 = this.get(x + xScale * 0, y + yScale * 0, z + zScale * 0);
                    final double v001 = this.get(x + xScale * 0, y + yScale * 0, z + zScale * 1);
                    final double v010 = this.get(x + xScale * 0, y + yScale * 1, z + zScale * 0);
                    final double v011 = this.get(x + xScale * 0, y + yScale * 1, z + zScale * 1);
                    final double v100 = this.get(x + xScale * 1, y + yScale * 0, z + zScale * 0);
                    final double v101 = this.get(x + xScale * 1, y + yScale * 0, z + zScale * 1);
                    final double v110 = this.get(x + xScale * 1, y + yScale * 1, z + zScale * 0);
                    final double v111 = this.get(x + xScale * 1, y + yScale * 1, z + zScale * 1);

                    double v0y0 = v000;
                    double v0y1 = v001;
                    double v1y0 = v100;
                    double v1y1 = v101;
                    final double d_dy__0y0 = (v010 - v000) * stepY;
                    final double d_dy__0y1 = (v011 - v001) * stepY;
                    final double d_dy__1y0 = (v110 - v100) * stepY;
                    final double d_dy__1y1 = (v111 - v101) * stepY;

                    for (int yRel = 0; yRel < yScale; ++yRel) {
                        double vxy0 = v0y0;
                        double vxy1 = v0y1;
                        final double d_dx__xy0 = (v1y0 - v0y0) * stepX;
                        final double d_dx__xy1 = (v1y1 - v0y1) * stepX;

                        // gradients start
                        double v0yz = v0y0;
                        double v1yz = v1y0;

                        final double d_dz__0yz = (v0y1 - v0y0) * stepX;
                        final double d_dz__1yz = (v1y1 - v1y0) * stepX;
                        // gradients end

                        for (int xRel = 0; xRel < xScale; ++xRel) {
                            final double d_dz__xyz = (vxy1 - vxy0) * stepZ;
                            double vxyz = vxy0;

                            // gradients start
                            final double d_dx__xyz = (v1yz - v0yz) * stepZ;
                            gradX[yRel][xRel] = d_dx__xyz; // for this one x and z are swapped
                            gradZ[xRel][yRel] = d_dz__xyz;
                            // gradients end
                            for (int zRel = 0; zRel < zScale; ++zRel) {
                                // to get gradients working, consumer usage moved to later
                                vals[xRel][yRel][zRel] = vxyz;
                                vxyz += d_dz__xyz;
                            }

                            vxy0 += d_dx__xy0;
                            vxy1 += d_dx__xy1;
                            // gradients start
                            v0yz += d_dz__0yz;
                            v1yz += d_dz__1yz;
                            // gradients end
                        }

                        v0y0 += d_dy__0y0;
                        v0y1 += d_dy__0y1;
                        v1y0 += d_dy__1y0;
                        v1y1 += d_dy__1y1;

                    }
                    // gradients start
                    double v00z = v000;
                    double v01z = v010;
                    double v10z = v100;
                    double v11z = v110;

                    final double d_dz__00z = (v001 - v000) * stepZ;
                    final double d_dz__01z = (v011 - v010) * stepZ;
                    final double d_dz__10z = (v101 - v100) * stepZ;
                    final double d_dz__11z = (v111 - v110) * stepZ;

                    for (int zRel = 0; zRel < zScale; ++zRel) {

                        double vx0z = v00z;
                        double vx1z = v01z;

                        final double d_dx__x0z = (v10z - v00z) * stepX;
                        final double d_dx__x1z = (v11z - v01z) * stepX;

                        for (int xRel = 0; xRel < xScale; ++xRel) {

                            double d_dy__xyz = (vx1z - vx0z) * stepY;

                            gradY[xRel][zRel] = d_dy__xyz;

                            vx0z += d_dx__x0z;
                            vx1z += d_dx__x1z;
                        }
                        v00z += d_dz__00z;
                        v01z += d_dz__01z;
                        v10z += d_dz__10z;
                        v11z += d_dz__11z;
                    }

                    for (int xRel = 0; xRel < xScale; ++xRel) {
                        for (int zRel = 0; zRel < zScale; ++zRel) {
                            for (int yRel = 0; yRel < yScale; ++yRel) {
                                double vxyz = vals[xRel][yRel][zRel];
                                double d_dx__xyz = gradX[yRel][zRel];
                                double d_dy__xyz = gradY[xRel][zRel];
                                double d_dz__xyz = gradZ[xRel][yRel];
                                consumer.accept(x + xRel, y + yRel, z + zRel, d_dx__xyz, d_dy__xyz, d_dz__xyz, vxyz);
                            }
                        }
                    }
                    // gradients end
                }
            }
        }
    }

    @Override
    public void populate(Cube cube) {
        if (PorkMethods.isCubeOutOfBounds(cube.getCoords())) {
            return;
        }

        if (!MinecraftForge.EVENT_BUS.post(new CubePopulatorEvent(world, cube))) {
            CubicBiome biome = CubicBiome.getCubic(cube.getCubicWorld().getBiome(Coords.getCubeCenter(cube)));

            CubePos pos = cube.getCoords();
            // For surface generators we should actually use special RNG with
            // seed
            // that depends only in world seed and cube X/Z
            // but using this for surface generation doesn't cause any
            // noticeable issues
            Random rand = new Random(cube.cubeRandomSeed());

            if (cube.getY() >= 1000 && cube.getY() <= 1015) {
                endChunkGenerator.populate(cube.getX(), cube.getZ());

                double j = rand.nextDouble();
                if (j > 0.9) {
                    BlockPos blockPos = new BlockPos(rand.nextInt(16) + cube.getCoords().getXCenter(), 16080, rand.nextInt(16) + cube.getCoords().getZCenter());
                    for (; blockPos.y > 16030; blockPos.y--) {
                        if (world.getBlockState(blockPos).getBlock() == Blocks.END_STONE) {
                            break;
                        }
                    }

                    if (blockPos.getY() > 16032) {
                        blockPos.y += 1;
                        BlockChorusFlower.generatePlant(World.class.cast(world), blockPos, rand, 8);
                    }
                }

                j = rand.nextDouble();
                if (j > 0.99) {
                    BlockPos blockPos = new BlockPos(rand.nextInt(16) + cube.getCoords().getXCenter(), 16080, rand.nextInt(16) + cube.getCoords().getZCenter());
                    for (; blockPos.y > 16030; blockPos.y--) {
                        if (world.getBlockState(blockPos).getBlock() == Blocks.END_STONE) {
                            break;
                        }
                    }

                    if (blockPos.getY() > 16032) {
                        blockPos.y += 1;
                        EntityShulker shulker = new EntityShulker(World.class.cast(world));
                        world.spawnEntity(shulker);
                        shulker.setPosition(blockPos.x + .5f, blockPos.y, blockPos.z + .5f);
                    }
                }

                return;
            }

            ICubicPopulator decorator = biome.getDecorator();
            decorator.generate(world, rand, pos, biome);
            CubeGeneratorsRegistry.generateWorld(world, rand, pos, biome);

            strongholds.generateStructure((World) world, rand, pos);
        }
    }
}
