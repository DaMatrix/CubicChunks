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
package cubicchunks.worldgen.generator;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubePrimer implements ICubePrimer {

    private final char[] data = new char[4096];

    @Override
    public IBlockState getBlockState(int x, int y, int z) {
        @SuppressWarnings("deprecation")
        IBlockState iblockstate = Block.BLOCK_STATE_IDS.getByValue(this.data[getBlockIndex(x, y, z)]);
        return iblockstate == null ? DEFAULT_STATE : iblockstate;
    }

    @Override
    public void setBlockState(int x, int y, int z, @Nonnull IBlockState state) {
        @SuppressWarnings("deprecation")
        char value = (char) Block.BLOCK_STATE_IDS.get(state);
        this.data[getBlockIndex(x, y, z)] = value;
    }

    @Override
    public void fill(IBlockState state) {
        @SuppressWarnings("deprecation")
        char c = (char) Block.BLOCK_STATE_IDS.get(state);
        for (int i = 0; i < 4096; i++)   {
            data[i] = c;
        }
    }

    /**
     * Map cube local coordinates to an array index in the range [0, 4095].
     *
     * @param x cube local x
     * @param y cube local y
     * @param z cube local z
     *
     * @return a unique array index for that coordinate
     */
    private static int getBlockIndex(int x, int y, int z) {
        return x << 8 | z << 4 | y;
    }
}
