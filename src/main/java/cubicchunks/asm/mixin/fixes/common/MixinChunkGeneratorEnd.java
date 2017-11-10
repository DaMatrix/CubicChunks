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

import net.minecraft.block.BlockChorusFlower;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkGeneratorEnd;
import net.minecraft.world.gen.feature.WorldGenEndGateway;
import net.minecraft.world.gen.feature.WorldGenEndIsland;
import net.minecraft.world.gen.structure.MapGenEndCity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(ChunkGeneratorEnd.class)
public abstract class MixinChunkGeneratorEnd {
    @Shadow @Final private boolean mapFeaturesEnabled;

    @Shadow private MapGenEndCity endCityGen;

    @Shadow @Final private World world;

    @Shadow @Final private Random rand;

    @Shadow protected abstract float getIslandHeightValue(int p_185960_1_, int p_185960_2_, int p_185960_3_, int p_185960_4_);

    @Shadow @Final private WorldGenEndIsland endIslands;

    @Shadow @Final private BlockPos spawnPoint;

    /**
     * asdf
     * @author asdf
     * @param x
     * @param z
     */
    @Overwrite
    public void populate(int x, int z) {
        BlockFalling.fallInstantly = true;
        net.minecraftforge.event.ForgeEventFactory.onChunkPopulate(true, ChunkGeneratorEnd.class.cast(this), this.world, this.rand, x, z, false);
        BlockPos blockpos = new BlockPos(x * 16, 16000, z * 16);

        if (this.mapFeaturesEnabled) {
            this.endCityGen.generateStructure(this.world, this.rand, new ChunkPos(x, z));
        }

        long i = (long) x * (long) x + (long) z * (long) z;

        if (i > 4096L) {
            float f = this.getIslandHeightValue(x, z, 1, 1);

            if (f < -20.0F && this.rand.nextInt(14) == 0) {
                this.endIslands.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16) + 8, 16055 + this.rand.nextInt(16), this.rand.nextInt(16) + 8));

                if (this.rand.nextInt(4) == 0) {
                    this.endIslands.generate(this.world, this.rand, blockpos.add(this.rand.nextInt(16) + 8, 16055 + this.rand.nextInt(16), this.rand.nextInt(16) + 8));
                }
            }

            if (this.getIslandHeightValue(x, z, 1, 1) > 40.0F) {
                int j = this.rand.nextInt(5);

                for (int k = 0; k < j; ++k) {
                    int l = this.rand.nextInt(16) + 8;
                    int i1 = this.rand.nextInt(16) + 8;
                    int j1 = this.world.getHeight(blockpos.add(l, 0, i1)).getY();

                    if (j1 > 0) {
                        int k1 = j1 - 1;

                        if (this.world.isAirBlock(blockpos.add(l, k1 + 1, i1)) && this.world.getBlockState(blockpos.add(l, k1, i1)).getBlock() == Blocks.END_STONE) {
                            BlockChorusFlower.generatePlant(this.world, blockpos.add(l, k1 + 1, i1), this.rand, 8);
                        }
                    }
                }

                if (this.rand.nextInt(700) == 0) {
                    int l1 = this.rand.nextInt(16) + 8;
                    int i2 = this.rand.nextInt(16) + 8;
                    int j2 = this.world.getHeight(blockpos.add(l1, 0, i2)).getY();

                    if (j2 > 0) {
                        int k2 = j2 + 3 + this.rand.nextInt(7);
                        BlockPos blockpos1 = blockpos.add(l1, k2, i2);
                        (new WorldGenEndGateway()).generate(this.world, this.rand, blockpos1);
                        TileEntity tileentity = this.world.getTileEntity(blockpos1);

                        if (tileentity instanceof TileEntityEndGateway) {
                            TileEntityEndGateway tileentityendgateway = (TileEntityEndGateway) tileentity;
                            tileentityendgateway.setExactPosition(this.spawnPoint);
                        }
                    }
                }
            }
        }

        net.minecraftforge.event.ForgeEventFactory.onChunkPopulate(false, ChunkGeneratorEnd.class.cast(this), this.world, this.rand, x, z, false);
        BlockFalling.fallInstantly = false;
    }
}
