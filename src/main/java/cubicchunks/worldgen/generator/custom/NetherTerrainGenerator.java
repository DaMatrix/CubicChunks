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

import cubicchunks.util.Box;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.BasicCubeGenerator;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.ICubePrimer;
import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import cubicchunks.worldgen.generator.custom.builder.NoiseSource;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMatcher;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.ChunkGeneratorHell;
import net.minecraft.world.gen.feature.WorldGenMinable;
import team.pepsi.ccaddon.PorkMethods;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

/**
 * Overrides the terrain generator for the nether:tm:
 * Makes it do cool things and stuff xd
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class NetherTerrainGenerator extends BasicCubeGenerator {
    public World vanillaWorld;
    public WorldGenMinable magmaGen = new WorldGenMinable(Blocks.MAGMA.getDefaultState(), 33, BlockMatcher.forBlock(Blocks.NETHERRACK));
    public WorldGenMinable quartzGen = new WorldGenMinable(Blocks.QUARTZ_ORE.getDefaultState(), 14, BlockMatcher.forBlock(Blocks.NETHERRACK));
    private Chunk chunkCache = null;
    private boolean optimizationHack = false;

    private IBuilder islandNoiseX;
    private IBuilder islandNoiseY;
    private IBuilder islandNoiseZ;

    public NetherTerrainGenerator(ICubicWorld world, final long seed) {
        super(world);

        vanillaWorld = (World) world;

        islandNoiseX = NoiseSource.perlin()
                .seed(seed)
                .frequency(0.005)
                .octaves(3)
                .normalizeTo(-1, 1)
                .create();

        islandNoiseY = NoiseSource.perlin()
                .seed(seed / 2)
                .frequency(0.03)
                .octaves(3)
                .normalizeTo(-1, 1)
                .create();

        islandNoiseZ = NoiseSource.perlin()
                .seed(seed * 2)
                .frequency(0.015)
                .octaves(3)
                .normalizeTo(-1, 1)
                .create();
    }

    @Override
    public void generateColumn(IColumn column) {
        byte[] columnBiomeArray = column.getBiomeArray();
        for (int i = 0; i < columnBiomeArray.length; ++i) {
            columnBiomeArray[i] = (byte) Biome.getIdForBiome(Biomes.HELL);
        }
    }

    @Override
    public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
        ICubePrimer primer = new CubePrimer();
        generate(primer, cubeX, cubeY, cubeZ);
        return primer;
    }

    @Override
    public void populate(Cube cube) {
        if (PorkMethods.isCubeOutOfBounds(cube.getCoords()))    {
            return;
        }

        Random random = new Random(cube.getX());
        BlockPos base = cube.getCoords().getCenterBlockPos();

        IBlockState lava = Blocks.LAVA.getDefaultState();
        for (int i = 0; i < 8; i++) {
            BlockPos pos = base.add(random.nextInt(16), random.nextInt(16), random.nextInt(16));
            if (world.getBlockState(pos).getBlock() == Blocks.NETHERRACK) {
                world.setBlockState(pos, lava, 2);
                vanillaWorld.immediateBlockTick(pos, lava, random);
            }
        }

        for (int i = 0; i < 6; i++) {
            quartzGen.generate(vanillaWorld, random, base.add(random.nextInt(16), random.nextInt(16), random.nextInt(16)));
        }

        if (cube.getY() < 4) {
            if (random.nextInt(4) == 1) {
                magmaGen.generate(vanillaWorld, random, base.add(random.nextInt(16), random.nextInt(16), random.nextInt(16)));
            }

            for (int i = 0; i < 8; i++) {
                BlockPos pos = base.add(random.nextInt(16), random.nextInt(16), random.nextInt(16));
                if (world.getBlockState(pos).getBlock() == Blocks.NETHERRACK) {
                    world.setBlockState(pos, lava, 2);
                    vanillaWorld.immediateBlockTick(pos, lava, random);
                }
            }
        }

        if (cube.getY() < 16 && cube.getY() > -1) {
            if (random.nextInt(16) == 1) {
                BlockPos gs = base.add(random.nextInt(16), 0, random.nextInt(16));
                boolean flag = false;
                for (int i = 0; i < 16; i++) {
                    if (world.getBlockState(gs).getBlock() == Blocks.NETHERRACK && world.getBlockState(gs.down()).getBlock() == Blocks.AIR) {
                        flag = true;
                        break;
                    }
                    gs.y++;
                }

                if (flag) {
                    for (int i = 0; i < 50; i++) {
                        BlockPos pos = gs.add(random.nextInt(14) - 7, -random.nextInt(5), random.nextInt(14) - 7);
                        if (world.isAirBlock(pos)) {
                            byte j = 0;

                            for (EnumFacing enumfacing : EnumFacing.values()) {
                                if (world.getBlockState(pos.offset(enumfacing)).getBlock() == Blocks.GLOWSTONE) {
                                    ++j;
                                }

                                if (j > 1) {
                                    break;
                                }
                            }

                            if (j == 1) {
                                world.setBlockState(pos, Blocks.GLOWSTONE.getDefaultState(), 2);
                            }
                        }
                    }
                }
            }

            if (random.nextInt(8) == 1) {
                IBlockState fire = Blocks.FIRE.getDefaultState();
                for (int i = 0; i < 32; i++)    {
                    BlockPos pos = base.add(random.nextInt(16), random.nextInt(16), random.nextInt(16));
                    if (world.isAirBlock(pos) && world.getBlockState(pos.down()).getBlock() == Blocks.NETHERRACK)   {
                        world.setBlockState(pos, fire, 2);
                    }
                }
            }
        }
    }

    @Override
    public Box getPopulationRequirement(Cube cube) {
        return RECOMMENDED_POPULATOR_REQUIREMENT;
    }

    @Override
    public void recreateStructures(Cube cube) {

    }

    @Nullable
    @Override
    public BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored) {
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
        if (PorkMethods.isCubeOutOfBounds(cubeZ))   {
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

        IBlockState netherrack = Blocks.NETHERRACK.getDefaultState();

        if (cubeY >= -2000 && cubeY <= 2000) { //Generate Nether
            boolean placeBlock;
            BlockPos pos = new BlockPos(cubeX * 16, cubeY * 16, cubeZ * 16);

            for (int x = 0; x < Cube.SIZE; x++) {
                int modifiedX = pos.x + x;
                for (int y = 0; y < Cube.SIZE; y++) {
                    int modifiedY = pos.y + y;
                    int factor = Math.abs(modifiedY);
                    double acceptanceThreshold = factor * shrinkFactor;
                    for (int z = 0; z < Cube.SIZE; z++) {
                        placeBlock = true;
                        int modifiedZ = pos.z + z;
                        double islandX = islandNoiseX.get(modifiedX, modifiedY, modifiedZ);
                        double islandY = islandNoiseY.get(modifiedX, modifiedY, modifiedZ);
                        double islandZ = islandNoiseZ.get(modifiedX, modifiedY, modifiedZ);
                        if (islandX + islandY + islandZ > acceptanceThreshold) {
                            placeBlock = false;
                        }

                        if (placeBlock) {
                            cubePrimer.setBlockState(x, y, z, netherrack);
                        }
                    }
                }
            }
            return;
        }

        for (int x = 0; x < Cube.SIZE; x++) {
            for (int y = 0; y < Cube.SIZE; y++) {
                for (int z = 0; z < Cube.SIZE; z++) {
                    cubePrimer.setBlockState(x, y, z, netherrack);
                }
            }
        }
    }

    private static double shrinkFactor = 1 - 0.999709371;
}
