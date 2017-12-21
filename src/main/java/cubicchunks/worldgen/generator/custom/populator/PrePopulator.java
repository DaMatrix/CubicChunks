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
package cubicchunks.worldgen.generator.custom.populator;

import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.api.worldgen.populator.ICubicPopulator;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.CustomGeneratorSettings;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PrePopulator implements ICubicPopulator {

    @Override public void generate(ICubicWorld world, Random random, CubePos pos, CubicBiome cubicBiome) {
        CustomGeneratorSettings cfg = CustomGeneratorSettings.fromJson(world.getWorldInfo().getGeneratorOptions());

        Biome biome = cubicBiome.getBiome();
        if (biome != Biomes.DESERT && biome != Biomes.DESERT_HILLS && cfg.waterLakes && random.nextInt(cfg.waterLakeRarity) == 0) {
            BlockPos blockPos = pos.randomPopulationPos(random);
            if (world.getBlockState(blockPos).getBlock() != Blocks.END_STONE) {
                (new WorldGenLakes(Blocks.WATER)).generate((World) world, random, blockPos);
            }
        }

        LAVA_LAKE:
        if (random.nextInt(cfg.lavaLakeRarity) == 0 && cfg.lavaLakes) {
            int yOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int xOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            int zOffset = random.nextInt(Cube.SIZE) + Cube.SIZE / 2;
            BlockPos blockPos = pos.getMinBlockPos().add(xOffset, yOffset, zOffset);
            if (world.getBlockState(blockPos).getBlock() == Blocks.END_STONE) {
                break LAVA_LAKE;
            }
            (new WorldGenLakes(Blocks.LAVA)).generate((World) world, random, blockPos);
        }

        if (cfg.dungeons) {
            for (int i = 0; i < cfg.dungeonCount; ++i) {
                BlockPos blockPos = pos.randomPopulationPos(random);
                if (world.getBlockState(blockPos).getBlock() != Blocks.END_STONE) {
                    (new WorldGenDungeons()).generate((World) world, random, blockPos);
                }
            }
        }
    }
}
