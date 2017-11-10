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

import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.structure.MapGenEndCity;
import net.minecraft.world.gen.structure.StructureEndCityPieces;
import net.minecraft.world.gen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(MapGenEndCity.Start.class)
public abstract class MixinEndCityStart extends StructureStart {
    @Shadow
    private boolean isSizeable;

    private static int getYPosForStructure(int p_191070_0_, int p_191070_1_, ChunkGeneratorEnd p_191070_2_) {
        Random random = new Random((long) (p_191070_0_ + p_191070_1_ * 10387313));
        Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        ChunkPrimer chunkprimer = new ChunkPrimer();
        p_191070_2_.setBlocksInChunk(p_191070_0_, p_191070_1_, chunkprimer);
        int i = 5;
        int j = 5;

        if (rotation == Rotation.CLOCKWISE_90) {
            i = -5;
        } else if (rotation == Rotation.CLOCKWISE_180) {
            i = -5;
            j = -5;
        } else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
            j = -5;
        }

        int k = chunkprimer.findGroundBlockIdx(7, 7);
        int l = chunkprimer.findGroundBlockIdx(7, 7 + j);
        int i1 = chunkprimer.findGroundBlockIdx(7 + i, 7);
        int j1 = chunkprimer.findGroundBlockIdx(7 + i, 7 + j);
        int k1 = Math.min(Math.min(k, l), Math.min(i1, j1));
        return k1;
    }

    /**
     * asdf
     *
     * @param worldIn
     * @param chunkProvider
     * @param rnd
     * @param chunkX
     * @param chunkZ
     * @author asdf
     */
    @Overwrite
    private void create(World worldIn, ChunkGeneratorEnd chunkProvider, Random rnd, int chunkX, int chunkZ) {
        Random random = new Random((long) (chunkX + chunkZ * 10387313));
        Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
        int i = getYPosForStructure(chunkX, chunkZ, chunkProvider) + 16000;

        if (i < 60) {
            this.isSizeable = false;
        } else {
            BlockPos blockpos = new BlockPos(chunkX * 16 + 8, i, chunkZ * 16 + 8);
            StructureEndCityPieces.startHouseTower(worldIn.getSaveHandler().getStructureTemplateManager(), blockpos, rotation, this.components, rnd);
            this.updateBoundingBox();
            this.isSizeable = true;
        }
    }
}
