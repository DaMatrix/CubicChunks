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

import cubicchunks.api.IKillDelayEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.pepsi.ccaddon.PorkMethods;

import javax.annotation.Nullable;

@Mixin(Entity.class)
public abstract class MixinEntity implements IKillDelayEntity {
    @Shadow
    public World world;

    private long addon_lastKillTime = System.currentTimeMillis();

    @Override
    public void setLastKill(long time) {
        addon_lastKillTime = time;
    }

    @Override
    public long lastKill() {
        return addon_lastKillTime;
    }

    @Shadow
    public boolean isDead;
    @Shadow
    public int dimension;
    @Shadow
    public double posX;
    @Shadow
    public double posZ;
    @Shadow
    public float rotationYaw;
    @Shadow
    public double posY;

    @Shadow
    @Nullable
    public abstract MinecraftServer getServer();

    @Shadow
    public abstract void setLocationAndAngles(double x, double y, double z, float yaw, float pitch);

    @Shadow
    public abstract void readFromNBT(NBTTagCompound compound);

    @Shadow public abstract void setPosition(double x, double y, double z);

    /**
     * ree
     *
     * @param dimensionIn
     * @return
     * @author uiwh
     */
    @Overwrite
    @Nullable
    public Entity changeDimension(int dimensionIn) {
        if (!this.world.isRemote && !this.isDead) {
            if (dimensionIn == 1)   {
                throw new IllegalStateException("Tried to change to end dimension!");
            }
            Entity this_ = Entity.class.cast(this);
            if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(this_, dimensionIn)) return null;
            this.world.profiler.startSection("changeDimension");
            MinecraftServer minecraftserver = this.getServer();
            int originalDim = this.dimension;
            WorldServer worldserver = minecraftserver.getWorld(originalDim);
            WorldServer worldserver1 = minecraftserver.getWorld(dimensionIn);
            this.dimension = dimensionIn;

            this.world.removeEntity(this_);
            this.isDead = false;
            this.world.profiler.startSection("reposition");
            BlockPos blockpos;

            if (dimensionIn == 1) {
                blockpos = worldserver1.getSpawnCoordinate();
            } else {
                double d0 = this.posX;
                double d1 = this.posZ;
                double d2 = this.posY;

                if (dimensionIn == -1) {
                    d0 = MathHelper.clamp(d0 / 8.0D, worldserver1.getWorldBorder().minX() + 16.0D, worldserver1.getWorldBorder().maxX() - 16.0D);
                    d1 = MathHelper.clamp(d1 / 8.0D, worldserver1.getWorldBorder().minZ() + 16.0D, worldserver1.getWorldBorder().maxZ() - 16.0D);
                    d2 = d2 / 8.0D;
                } else if (dimensionIn == 0) {
                    d0 = MathHelper.clamp(d0 * 8.0D, worldserver1.getWorldBorder().minX() + 16.0D, worldserver1.getWorldBorder().maxX() - 16.0D);
                    d1 = MathHelper.clamp(d1 * 8.0D, worldserver1.getWorldBorder().minZ() + 16.0D, worldserver1.getWorldBorder().maxZ() - 16.0D);
                    d2 = d2 * 8.0D;
                }

                d0 = (double) MathHelper.clamp((int) d0, -29999872, 29999872);
                d1 = (double) MathHelper.clamp((int) d1, -29999872, 29999872);
                float f = this.rotationYaw;
                this.setLocationAndAngles(d0, d2, d1, 90.0F, 0.0F);
                Teleporter teleporter = worldserver1.getDefaultTeleporter();
                teleporter.placeInExistingPortal(this_, f);
                blockpos = new BlockPos(this_);
            }

            worldserver.updateEntityWithOptionalForce(this_, false);
            this.world.profiler.endStartSection("reloading");
            Entity entity = EntityList.newEntity(this_.getClass(), worldserver1);

            if (entity != null) {
                entity.copyDataFromOld(this_);

                if (originalDim == 1 && dimensionIn == 1) {
                    BlockPos blockpos1 = worldserver1.getTopSolidOrLiquidBlock(worldserver1.getSpawnPoint());
                    entity.moveToBlockPosAndAngles(blockpos1, entity.rotationYaw, entity.rotationPitch);
                } else {
                    entity.moveToBlockPosAndAngles(blockpos, entity.rotationYaw, entity.rotationPitch);
                }

                boolean flag = entity.forceSpawn;
                entity.forceSpawn = true;
                worldserver1.spawnEntity(entity);
                entity.forceSpawn = flag;
                worldserver1.updateEntityWithOptionalForce(entity, false);
            }

            this.isDead = true;
            this.world.profiler.endSection();
            worldserver.resetUpdateEntityTick();
            worldserver1.resetUpdateEntityTick();
            this.world.profiler.endSection();
            return entity;
        } else {
            return null;
        }
    }

    @Inject(method = "Lnet/minecraft/entity/Entity;onEntityUpdate()V", at = @At("RETURN"))
    public void postEntityUpdate(CallbackInfo callbackInfo) {
        if (!world.isRemote) {
            if (this.posZ < 0) {
                this.setPosition(this.posX, this.posY, 0);
            } else if (this.posZ > 255) {
                this.setPosition(this.posX, this.posY, 255);
            }
        }
    }
}
