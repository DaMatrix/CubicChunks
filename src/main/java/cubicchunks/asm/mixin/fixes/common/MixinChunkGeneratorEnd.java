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

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.feature.WorldGenEndIsland;
import net.minecraft.world.gen.structure.MapGenEndCity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(ChunkGeneratorEnd.class)
public abstract class MixinChunkGeneratorEnd {
    @Shadow
    @Final
    private boolean mapFeaturesEnabled;

    @Shadow
    private MapGenEndCity endCityGen;

    @Shadow
    @Final
    private World world;

    @Shadow
    @Final
    private Random rand;
    @Shadow
    @Final
    private WorldGenEndIsland endIslands;
    @Shadow
    @Final
    private BlockPos spawnPoint;

    @Shadow
    protected abstract float getIslandHeightValue(int p_185960_1_, int p_185960_2_, int p_185960_3_, int p_185960_4_);

    /**
     * asdf
     *
     * @param x
     * @param z
     * @author asdf
     */
    @Overwrite
    public void populate(int x, int z) {
        if (Math.abs((x * 16) ^ 2 + (z * 16) ^ 2) > 4096) {
            this.endCityGen.generateStructure(this.world, this.rand, new ChunkPos(x, z));

            if (this.rand.nextInt(50) == 0) {
                this.endIslands.generate(this.world, this.rand, new BlockPos(x * 16 + this.rand.nextInt(16) + 8, 16055 + this.rand.nextInt(32), z * 16 + this.rand.nextInt(16) + 8));

                if (this.rand.nextInt(14) == 0) {
                    this.endIslands.generate(this.world, this.rand, new BlockPos(x * 16 + this.rand.nextInt(16) + 8, 16055 + this.rand.nextInt(16), z * 16 + this.rand.nextInt(16) + 8));
                }
            }
        }
    }
}
