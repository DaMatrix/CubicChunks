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

import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import team.pepsi.ccaddon.PorkMethods;

@Mixin(BlockEndPortal.class)
public abstract class MixinBlockEndPortal {
    /**
     * alkfh
     *
     * @author dkfhawiuh3uzg
     */
    @Overwrite
    public void onEntityCollidedWithBlock(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
        if (!worldIn.isRemote && entityIn.dimension != 0)    {
            if (entityIn instanceof EntityPlayerMP)   {
                ((EntityPlayerMP) entityIn).connection.disconnect(new TextComponentString("\u00A7cUsing an end portal in invalid dimension."));
            } else {
                entityIn.setDead();
            }
            return;
        }

        if (!worldIn.isRemote && !entityIn.isRiding() && !entityIn.isBeingRidden() && entityIn.isNonBoss() && entityIn.getEntityBoundingBox().intersects(state.getBoundingBox(worldIn, pos).offset(pos))) {
            BlockPos spawnPoint;
            CHECK: if (entityIn.posY > 16000)  {
                if (entityIn instanceof EntityPlayer)   {
                    EntityPlayer player = (EntityPlayer) entityIn;
                        spawnPoint = player.getBedLocation();
                        if (spawnPoint != null)
                        {
                            BlockPos blockpos1 = EntityPlayer.getBedSpawnLocation(FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(player.dimension), spawnPoint, player.isSpawnForced());

                            if (blockpos1 != null)
                            {
                                spawnPoint = blockpos1;
                                break CHECK;
                            }
                            else
                            {
                                ((EntityPlayerMP) player).connection.sendPacket(new SPacketChangeGameState(0, 0.0F));
                            }
                        }

                }

                spawnPoint = PorkMethods.getSafeSpawnPoint(worldIn,
                        new BlockPos(0, PorkMethods.overworldSpawnOffset,  0),
                        128,256, 120);
            } else {
                spawnPoint = PorkMethods.getSafeSpawnPoint(worldIn,
                        new BlockPos(0, 16040,  0),
                        128,256, 120);
            }
            entityIn.setPositionAndUpdate(spawnPoint.x + 0.5, spawnPoint.y, spawnPoint.z + 0.5);
        }
    }
}
