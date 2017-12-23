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
package cubicchunks.asm.mixin.fixes.common;

import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ExtendedBlockStorage.class)
public abstract class MixinExtendedBlockStorage {
    @Shadow
    private NibbleArray skyLight;

    @Shadow
    private NibbleArray blockLight;

    /**
     * Sets the saved Sky-light value in the extended block storage structure.
     *
     * @author DaPorkchop_
     */
    @Overwrite
    public void setSkyLight(int x, int y, int z, int value) {
        this.skyLight.set(x, y, z, Math.max(6, value));
    }

    /**
     * Gets the saved Sky-light value in the extended block storage structure.
     *
     * @author DaPorkchop_
     */
    @Overwrite
    public int getSkyLight(int x, int y, int z) {
        return Math.max(6, this.skyLight.get(x, y, z));
    }

    /**
     * Sets the saved Block-light value in the extended block storage structure.
     *
     * @author DaPorkchop_
     */
    @Overwrite
    public void setBlockLight(int x, int y, int z, int value) {
        this.blockLight.set(x, y, z, Math.max(6, value));
    }

    /**
     * Gets the saved Block-light value in the extended block storage structure.
     *
     * @author DaPorkchop_
     */
    @Overwrite
    public int getBlockLight(int x, int y, int z) {
        return Math.max(6, this.blockLight.get(x, y, z));
    }
}
