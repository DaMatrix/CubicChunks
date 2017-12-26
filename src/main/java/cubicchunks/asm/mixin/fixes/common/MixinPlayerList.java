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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketChangeGameState;
import net.minecraft.network.play.server.SPacketRespawn;
import net.minecraft.network.play.server.SPacketSetExperience;
import net.minecraft.network.play.server.SPacketSpawnPosition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.DemoPlayerInteractionManager;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import team.pepsi.ccaddon.PorkMethods;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class MixinPlayerList {
    @Shadow
    @Final
    private MinecraftServer mcServer;

    @Shadow
    @Final
    private List<EntityPlayerMP> playerEntityList;
    @Shadow
    @Final
    private Map<UUID, EntityPlayerMP> uuidToPlayerMap;

    @Shadow
    protected abstract void setPlayerGameTypeBasedOnOther(EntityPlayerMP target, EntityPlayerMP source, World worldIn);

    @Shadow
    public abstract void updateTimeAndWeatherForPlayer(EntityPlayerMP playerIn, WorldServer worldIn);

    @Shadow
    public abstract void updatePermissionLevel(EntityPlayerMP player);

    /**
     * ahd
     *
     * @param playerIn
     * @param dimension
     * @param conqueredEnd
     * @return
     * @author xkdh
     */
    @Overwrite
    public EntityPlayerMP recreatePlayerEntity(EntityPlayerMP playerIn, int dimension, boolean conqueredEnd) {
        World world = mcServer.getWorld(dimension);
        if (world == null) {
            dimension = playerIn.getSpawnDimension();
        } else if (!world.provider.canRespawnHere()) {
            dimension = world.provider.getRespawnDimension(playerIn);
        }
        if (mcServer.getWorld(dimension) == null) dimension = 0;

        playerIn.getServerWorld().getEntityTracker().removePlayerFromTrackers(playerIn);
        playerIn.getServerWorld().getEntityTracker().untrack(playerIn);
        playerIn.getServerWorld().getPlayerChunkMap().removePlayer(playerIn);
        this.playerEntityList.remove(playerIn);
        this.mcServer.getWorld(playerIn.dimension).removeEntityDangerously(playerIn);
        BlockPos blockpos = playerIn.getBedLocation(dimension);
        boolean flag = playerIn.isSpawnForced(dimension);
        playerIn.dimension = dimension;
        PlayerInteractionManager playerinteractionmanager;

        if (this.mcServer.isDemo()) {
            playerinteractionmanager = new DemoPlayerInteractionManager(this.mcServer.getWorld(playerIn.dimension));
        } else {
            playerinteractionmanager = new PlayerInteractionManager(this.mcServer.getWorld(playerIn.dimension));
        }

        EntityPlayerMP entityplayermp = new EntityPlayerMP(this.mcServer, this.mcServer.getWorld(playerIn.dimension), playerIn.getGameProfile(), playerinteractionmanager);
        entityplayermp.connection = playerIn.connection;
        entityplayermp.copyFrom(playerIn, conqueredEnd);
        entityplayermp.dimension = dimension;
        entityplayermp.setEntityId(playerIn.getEntityId());
        entityplayermp.setCommandStats(playerIn);
        entityplayermp.setPrimaryHand(playerIn.getPrimaryHand());

        for (String s : playerIn.getTags()) {
            entityplayermp.addTag(s);
        }

        WorldServer worldserver = this.mcServer.getWorld(playerIn.dimension);
        this.setPlayerGameTypeBasedOnOther(entityplayermp, playerIn, worldserver);

        if (blockpos == null) {
            BlockPos blockpos1 = PorkMethods.getSafeSpawnPoint(worldserver, worldserver.getSpawnPoint(), 128, 256, 120);

            entityplayermp.setLocationAndAngles((double) ((float) blockpos1.getX() + 0.5F), (double) ((float) blockpos1.getY() + 0.1F), (double) ((float) blockpos1.getZ() + 0.5F), 0.0F, 0.0F);
            entityplayermp.setSpawnPoint(blockpos, flag);
        } else {
            BlockPos blockpos1 = EntityPlayer.getBedSpawnLocation(this.mcServer.getWorld(playerIn.dimension), blockpos, flag);

            if (blockpos1 != null) {
                entityplayermp.setLocationAndAngles((double) ((float) blockpos1.getX() + 0.5F), (double) ((float) blockpos1.getY() + 0.1F), (double) ((float) blockpos1.getZ() + 0.5F), 0.0F, 0.0F);
                entityplayermp.setSpawnPoint(blockpos, flag);
            } else {
                entityplayermp.connection.sendPacket(new SPacketChangeGameState(0, 0.0F));
            }
        }

        worldserver.getChunkProvider().provideChunk((int) entityplayermp.posX >> 4, (int) entityplayermp.posZ >> 4);

        while (!worldserver.getCollisionBoxes(entityplayermp, entityplayermp.getEntityBoundingBox()).isEmpty()) {
            entityplayermp.setPosition(entityplayermp.posX, entityplayermp.posY + 1.0D, entityplayermp.posZ);
        }

        entityplayermp.connection.sendPacket(new SPacketRespawn(entityplayermp.dimension, entityplayermp.world.getDifficulty(), entityplayermp.world.getWorldInfo().getTerrainType(), entityplayermp.interactionManager.getGameType()));
        BlockPos blockpos2 = worldserver.getSpawnPoint();
        entityplayermp.connection.setPlayerLocation(entityplayermp.posX, entityplayermp.posY, entityplayermp.posZ, entityplayermp.rotationYaw, entityplayermp.rotationPitch);
        entityplayermp.connection.sendPacket(new SPacketSpawnPosition(blockpos2));
        entityplayermp.connection.sendPacket(new SPacketSetExperience(entityplayermp.experience, entityplayermp.experienceTotal, entityplayermp.experienceLevel));
        this.updateTimeAndWeatherForPlayer(entityplayermp, worldserver);
        this.updatePermissionLevel(entityplayermp);
        worldserver.getPlayerChunkMap().addPlayer(entityplayermp);
        worldserver.spawnEntity(entityplayermp);
        this.playerEntityList.add(entityplayermp);
        this.uuidToPlayerMap.put(entityplayermp.getUniqueID(), entityplayermp);
        entityplayermp.addSelfToInternalCraftingInventory();
        entityplayermp.setHealth(entityplayermp.getHealth());
        net.minecraftforge.fml.common.FMLCommonHandler.instance().firePlayerRespawnEvent(entityplayermp, conqueredEnd);
        return entityplayermp;
    }

    /**
     * asdf
     *
     * @author DaPorkchop_
     */
    //@Overwrite
    public void transferEntityToWorld(Entity entityIn, int lastDimension, WorldServer oldWorldIn, WorldServer toWorldIn, net.minecraft.world.Teleporter teleporter)
    {
        net.minecraft.world.WorldProvider pOld = oldWorldIn.provider;
        net.minecraft.world.WorldProvider pNew = toWorldIn.provider;
        double moveFactor = pOld.getMovementFactor() / pNew.getMovementFactor();
        double d0 = entityIn.posX * moveFactor;
        double d1 = 128d;
        double d2 = entityIn.posY * moveFactor;
        float f = entityIn.rotationYaw;
        oldWorldIn.profiler.startSection("moving");

        if (false && entityIn.dimension == -1) {
            d0 = MathHelper.clamp(d0 / 8.0D, toWorldIn.getWorldBorder().minX() + 16.0D, toWorldIn.getWorldBorder().maxX() - 16.0D);
            //d1 = MathHelper.clamp(d1 / 8.0D, toWorldIn.getWorldBorder().minZ() + 16.0D, toWorldIn.getWorldBorder().maxZ() - 16.0D);
            d2 = d2 / 8.0d;
            entityIn.setLocationAndAngles(d0, d2, d1, entityIn.rotationYaw, entityIn.rotationPitch);

            if (entityIn.isEntityAlive()) {
                oldWorldIn.updateEntityWithOptionalForce(entityIn, false);
            }
        } else if (false && entityIn.dimension == 0) {
            d0 = MathHelper.clamp(d0 * 8.0D, toWorldIn.getWorldBorder().minX() + 16.0D, toWorldIn.getWorldBorder().maxX() - 16.0D);
            //d1 = MathHelper.clamp(d1 * 8.0D, toWorldIn.getWorldBorder().minZ() + 16.0D, toWorldIn.getWorldBorder().maxZ() - 16.0D);
            entityIn.setLocationAndAngles(d0, d2, d1, entityIn.rotationYaw, entityIn.rotationPitch);

            if (entityIn.isEntityAlive()) {
                oldWorldIn.updateEntityWithOptionalForce(entityIn, false);
            }
        }

        if (entityIn.dimension == 1) {
            BlockPos blockpos;

            if (lastDimension == 1) {
                blockpos = toWorldIn.getSpawnPoint();
            } else {
                blockpos = toWorldIn.getSpawnCoordinate();
            }

            d0 = (double) blockpos.getX();
            entityIn.posY = (double) blockpos.getY();
            d1 = (double) blockpos.getZ();
            entityIn.setLocationAndAngles(d0, entityIn.posY, d1, 90.0F, 0.0F);

            if (entityIn.isEntityAlive()) {
                oldWorldIn.updateEntityWithOptionalForce(entityIn, false);
            }
        }

        oldWorldIn.profiler.endSection();

        if (lastDimension != 1) {
            oldWorldIn.profiler.startSection("placing");
            d0 = (double) MathHelper.clamp((int) d0, -29999872, 29999872);
            d1 = (double) MathHelper.clamp((int) d1, 0, 255);

            if (entityIn.isEntityAlive()) {
                entityIn.setLocationAndAngles(d0, d2, d1, entityIn.rotationYaw, entityIn.rotationPitch);
                teleporter.placeInPortal(entityIn, f);
                toWorldIn.spawnEntity(entityIn);
                toWorldIn.updateEntityWithOptionalForce(entityIn, false);
            }

            oldWorldIn.profiler.endSection();
        }

        entityIn.setWorld(toWorldIn);
    }
}