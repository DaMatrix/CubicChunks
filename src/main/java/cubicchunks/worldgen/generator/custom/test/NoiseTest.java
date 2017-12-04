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

import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import cubicchunks.worldgen.generator.custom.builder.NoiseSource;

public class NoiseTest {
    public static void main(String... args) {
        IBuilder builder = NoiseSource.perlin()
                .frequency(0.06)
                .octaves(6)
                .normalizeTo(-1, 1)
                .seed(System.currentTimeMillis())
                .create();

        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (int x = -100000; x < 900000; x++)    {
            double valueX = builder.get(x, 0, 0) * 256;
            double valueY = builder.get(0, x, 0) * 256;
            double valueZ = builder.get(0, 0, x) * 256;
            double value = (valueX + valueY + valueZ) / 3;
            if (value < min)    {
                min = value;
            } else if (value > max) {
                max = value;
            }
            System.out.println((value > 0) + " " + (valueX > 0) + " " + (valueY > 0) + " " + (valueZ > 0));
        }
        System.out.println(min + " " + max);
    }
}
