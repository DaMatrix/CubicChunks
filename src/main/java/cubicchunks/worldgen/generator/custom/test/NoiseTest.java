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
package cubicchunks.worldgen.generator.custom.test;

import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import cubicchunks.worldgen.generator.custom.builder.NoiseSource;
import net.minecraft.util.math.BlockPos;

public class NoiseTest {
    private static double shrinkFactor = 1 - 0.998409371;

    public static void main(String... args) {
        IBuilder builder = NoiseSource.perlin()
                .frequency(0.06)
                .octaves(6)
                .normalizeTo(-1, 1)
                .seed(System.currentTimeMillis() * 2)
                .create();
        IBuilder builder1 = NoiseSource.perlin()
                .frequency(0.06)
                .octaves(6)
                .normalizeTo(-1, 1)
                .seed(System.currentTimeMillis() / 2)
                .create();
        IBuilder builder2 = NoiseSource.perlin()
                .frequency(0.06)
                .octaves(6)
                .normalizeTo(-1, 1)
                .seed(System.currentTimeMillis())
                .create();

        boolean placeBlock;
        int filled = 0, empty = 0;
        BlockPos pos = new BlockPos(0, -5, 0);
        for (int i = 0; i < 10; i++) {
            for (int x = 0; x < Cube.SIZE; x++) {
                int modifiedX = pos.x + x;
                for (int y = 0; y < Cube.SIZE; y++) {
                    int modifiedY = pos.y + y;
                    int factor = Math.abs(modifiedY);
                    double acceptanceThreshold = factor * shrinkFactor;
                    for (int z = 0; z < Cube.SIZE; z++) {
                        placeBlock = true;
                        int modifiedZ = pos.z + z;
                        double islandX = builder.get(modifiedX, modifiedY, modifiedZ);
                        double islandY = builder1.get(modifiedX, modifiedY, modifiedZ);
                        double islandZ = builder2.get(modifiedX, modifiedY, modifiedZ);
                        if (islandX + islandY + islandZ > acceptanceThreshold) {
                            placeBlock = false;
                        }

                        if (placeBlock) {
                            filled++;
                        } else {
                            empty++;
                        }
                    }
                }
            }
            pos.y += 16;
        }
        System.out.println(filled + " " + empty);
    }
}
