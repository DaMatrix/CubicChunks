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
    private final ChunkGeneratorHell netherGen;
    public World vanillaWorld;
    public WorldGenMinable magmaGen = new WorldGenMinable(Blocks.MAGMA.getDefaultState(), 33, BlockMatcher.forBlock(Blocks.NETHERRACK));
    public WorldGenMinable quartzGen = new WorldGenMinable(Blocks.QUARTZ_ORE.getDefaultState(), 14, BlockMatcher.forBlock(Blocks.NETHERRACK));
    private Chunk chunkCache = null;
    private boolean optimizationHack = false;

    public NetherTerrainGenerator(ICubicWorld world, final long seed) {
        super(world);

        vanillaWorld = (World) world;

        this.netherGen = new ChunkGeneratorHell((World) world, true, seed);
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
        netherGen.populate(cube.getX(), cube.getZ());
        Random random = new Random(cube.getX());
        BlockPos base = cube.getCoords().getCenterBlockPos();

        IBlockState lava = Blocks.LAVA.getDefaultState();
        IBlockState netherrack = Blocks.NETHERRACK.getDefaultState();
        for (int i = 0; i < 8; i++) {
            BlockPos pos = base.add(random.nextInt(16), random.nextInt(16), random.nextInt(16));
            if (world.getBlockState(pos).getBlock() == Blocks.NETHERRACK) {
                world.setBlockState(pos, lava, 2);
                vanillaWorld.immediateBlockTick(pos, lava, random);
            }
        }

        quartzGen.generate(vanillaWorld, random, base.add(random.nextInt(16), random.nextInt(16), random.nextInt(16)));

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
        IBlockState netherrack = Blocks.NETHERRACK.getDefaultState();

        if (cubeY >= 0 && cubeY <= 7) { //Generate Nether
            IBlockState bedrock = Blocks.BEDROCK.getDefaultState();
            Chunk chunk = chunkCache;
            if (chunk == null || chunk.x != cubeX || chunk.z != cubeZ) {
                chunk = netherGen.generateChunk(cubeX, cubeZ);
                chunkCache = chunk;
            }
            if (!optimizationHack) {
                optimizationHack = true;
                // Recursive generation
                for (int y = 0; y < 8; y++) {
                    if (y == cubeY) {
                        continue;
                    }
                    world.getCubeFromCubeCoords(cubeX, y, cubeZ);
                }
                optimizationHack = false;
            }

            ExtendedBlockStorage storage = chunk.getBlockStorageArray()[cubeY];
            if (storage != null && !storage.isEmpty()) {
                for (int x = 0; x < Cube.SIZE; x++) {
                    for (int y = 0; y < Cube.SIZE; y++) {
                        for (int z = 0; z < Cube.SIZE; z++) {
                            IBlockState state = storage.get(x, y, z);
                            if (state == bedrock) {
                                cubePrimer.setBlockState(x, y, z, netherrack);
                            } else {
                                cubePrimer.setBlockState(x, y, z, state);
                            }
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
}
