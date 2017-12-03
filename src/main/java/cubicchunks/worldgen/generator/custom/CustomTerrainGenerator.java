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
import java.util.concurrent.ExecutionException;
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
    private LoadingCache<Long, Integer[][]> groundNoiseCache = CacheBuilder.<Long, Integer[][]>newBuilder()
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .maximumSize(512)
            .build(new CacheLoader<Long, Integer[][]>() {
                @Override
                public Integer[][] load(Long key) {
                    int x = (int) (key >> 32);
                    int y = (int) (long) key;
                    x *= 16;
                    y *= 16;
                    Integer[][] heights = new Integer[16][16];
                    for (int genX = 0; genX < heights.length; genX++) {
                        Integer[] a = heights[genX];
                        for (int genY = 0; genY < a.length; genY++) {
                            a[genY] = (int) (groundNoise.getValue(x + genX, y + genY, 0) * 256);
                        }
                    }
                    return heights;
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

    private IBlockState chooseState(Random random, IBlockState... states) {
        return states[random.nextInt(states.length)];
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

        try {
            Integer[][] groundNoiseArr = groundNoiseCache.get((((long) cubeX) << 32) | (cubeZ & 0xffffffffL));
            int cubeAbsoluteX = cubeX * 16;
            int cubeAbsoluteY = cubeY * 16;
            int cubeAbsoluteZ = cubeZ * 16;
            IBlockState air = Blocks.AIR.getDefaultState();
            for (int x = 0, absX = cubeAbsoluteX; x < Cube.SIZE; x++, absX++) {
                Integer[] groundNoiseAtX = groundNoiseArr[x];
                for (int z = 0, absZ = cubeAbsoluteZ; z < Cube.SIZE; z++, absZ++) {
                    int groundNoise = groundNoiseAtX[z];
                    for (int y = 0, absY = cubeAbsoluteY; absY < groundNoise; y++, absY++) {
                        List<IBiomeBlockReplacer> replacers = biomeSource.getReplacers(absX, absY, absZ);
                        IBlockState block = Blocks.AIR.getDefaultState();
                        int size = replacers.size();
                        double density = groundNoise - absY;
                        for (int i = 0; i < size; i++) {
                            block = replacers.get(i).getReplacedBlock(block, absX, absY, absZ, /*dx, dy, dz, density*/1, 1, 1, density);
                        }

                        if (block != air) {
                            cubePrimer.setBlockState(x, y, z, block);
                        }
                    }
                }
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the blockstate appropriate for the specified builder entry
     *
     * @return The block state
     */
    private IBlockState getBlock(int x, int y, int z, double dx, double dy, double dz, double density) {
        List<IBiomeBlockReplacer> replacers = biomeSource.getReplacers(x, y, z);
        IBlockState block = Blocks.AIR.getDefaultState();
        int size = replacers.size();
        for (int i = 0; i < size; i++) {
            block = replacers.get(i).getReplacedBlock(block, x, y, z, dx, dy, dz, density);
        }
        return block;
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
}
