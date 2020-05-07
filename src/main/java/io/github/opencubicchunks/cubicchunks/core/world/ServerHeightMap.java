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
package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import mcp.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ServerHeightMap implements IHeightMap {
    public ServerHeightMap(int[] heightmap) {
    }

    // Interface: IHeightMap ----------------------------------------------------------------------------------------

    @Override
    public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
    }

    @Override
    public boolean isOccluded(int localX, int blockY, int localZ) {
        return false;
    }

    @Override
    public int getTopBlockY(int localX, int localZ) {
        return Integer.MIN_VALUE;
    }

    @Override
    public int getTopBlockYBelow(int localX, int localZ, int blockY) {
        return Integer.MIN_VALUE;
    }

    @Override
    public int getLowestTopBlockY() {
        return Integer.MIN_VALUE;
    }

    // Serialization / NBT ---------------------------------------------------------------------------------------------

    public byte[] getData() {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(Cube.SIZE * Cube.SIZE * (4 + 4 + 2));
        try {
            for (int i = 0; i < Cube.SIZE * Cube.SIZE; i++) {
                buf.writeInt(Integer.MIN_VALUE)
                        .writeInt(Integer.MIN_VALUE)
                        .writeShort(0);
            }
            byte[] arr = new byte[buf.readableBytes()];
            buf.readBytes(arr);
            return arr;
        } finally {
            buf.release();
        }
    }

    public byte[] getDataForClient() {
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.ioBuffer(Cube.SIZE * Cube.SIZE * 4);
        try {
            for (int i = 0; i < Cube.SIZE * Cube.SIZE; i++) {
                buf.writeInt(Integer.MIN_VALUE);
            }
            byte[] arr = new byte[buf.readableBytes()];
            buf.readBytes(arr);
            return arr;
        } finally {
            buf.release();
        }
    }

    public void readData(byte[] data) {
    }
}
