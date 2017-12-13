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
package cubicchunks.asm.mixin.core.client;

import cubicchunks.util.ClassInheritanceMultiMapFactory;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import team.pepsi.ccaddon.PorkMethods;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Iterator;
import java.util.List;

import static cubicchunks.asm.JvmNames.*;

/**
 * Fixes renderEntities crashing when rendering cubes
 * that are not at existing array index in chunk.getEntityLists(),
 * <p>
 * Allows to render cubes outside of 0..256 height range.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Nullable private BlockPos position;

    @Shadow private int renderDistanceChunks;

    @Shadow private ViewFrustum viewFrustum;

    @Shadow @Final private TextureManager renderEngine;

    @Shadow @Final private Minecraft mc;

    @Shadow private WorldClient world;

    @Shadow @Final private static ResourceLocation FORCEFIELD_TEXTURES;

    /**
     * This allows to get the Y position of rendered entity by injecting itself directly before call to
     * chunk.getEntityLists
     */
    @Group(name = "renderEntitiesFix")//, min = 3, max = 3)
    @Inject(method = "renderEntities",
            at = @At(value = "INVOKE", target = WORLD_CLIENT_GET_CHUNK_FROM_BLOCK_COORDS),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void onGetPosition(Entity renderViewEntity, ICamera camera, float partialTicks,
            CallbackInfo ci, int pass, double d0, double d1, double d2,
            Entity entity, double d3, double d4, double d5,
            List<Entity> list, List<Entity> list1, List<Entity> list2,
            BlockPos.PooledMutableBlockPos pos, Iterator<RenderGlobal.ContainerLocalRenderInformation> var21,
            RenderGlobal.ContainerLocalRenderInformation info) {
        ICubicWorld world = (ICubicWorld) info.renderChunk.getWorld();
        if (world.isCubicWorld()) {
            this.position = info.renderChunk.getPosition();
        } else {
            this.position = null;
        }
    }

    /**
     * Optifine-specific version of the above method. Up to version 1.12.2_HD_U_C6
     */
    @SuppressWarnings("UnresolvedMixinReference")
    @Group(name = "renderEntitiesFix")
    @Inject(method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getChunk(Lnet/minecraft/world/World;)Lnet/minecraft/world/chunk/Chunk;"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false)
    public void onGetPositionOptifine_Old(Entity renderViewEntity, ICamera camera, float partialTicks,
            CallbackInfo ci, int pass, double d0, double d1, double d2,
            Entity entity, double d3, double d4, double d5,
            List list, boolean forgeEntityPass, boolean forgeTileEntityPass, boolean isShaders, boolean oldFancyGraphics, List list1, List list2,
            BlockPos.PooledMutableBlockPos pos, Iterator iterInfosEntities,
            RenderGlobal.ContainerLocalRenderInformation info) {
        ICubicWorld world = (ICubicWorld) info.renderChunk.getWorld();
        if (world.isCubicWorld()) {
            this.position = info.renderChunk.getPosition();
        } else {
            this.position = null;
        }
    }

    /**
     * Optifine-specific version of the above method. Versions 1.12.2_HD_U_C7_pre and up
     */
    @SuppressWarnings("UnresolvedMixinReference")
    @Group(name = "renderEntitiesFix")
    @Inject(method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getChunk()Lnet/minecraft/world/chunk/Chunk;"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false)
    public void onGetPositionOptifine_New(Entity renderViewEntity, ICamera camera, float partialTicks,
            CallbackInfo ci, int pass, double d0, double d1, double d2,
            Entity entity, double d3, double d4, double d5,
            List list, boolean forgeEntityPass, boolean forgeTileEntityPass, boolean isShaders, boolean oldFancyGraphics, List list1, List list2,
            BlockPos.PooledMutableBlockPos pos, Iterator var22, RenderGlobal.ContainerLocalRenderInformation info) {
        ICubicWorld world = (ICubicWorld) info.renderChunk.getWorld();
        if (world.isCubicWorld()) {
            this.position = info.renderChunk.getPosition();
        } else {
            this.position = null;
        }
    }

    /**
     * After chunk.getEntityLists() renderGlobal needs to get correct element of the array.
     * The array element number is calculated using renderChunk.getPosition().getY() / 16.
     * getY() is redirected to this method to always return 0.
     * <p>
     * Then chunk.getEntityLists is redirected to a method that returns a 1-element array.
     */
    @Group(name = "renderEntitiesFix")
    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = BLOCK_POS_GETY), require = 1)
    public int getRenderChunkYPos(BlockPos pos) {
        //position is null when it's not cubic chunks renderer
        if (this.position != null) {
            return 0;//must be 0 (or anything between 0 and 15)
        }
        return pos.getY();
    }

    /**
     * Return a 1-element array for Cubic Chunks world,
     * or original chunk.getEntityLists if not cubic chunks world.
     */
    @SuppressWarnings("unchecked")
    @Group(name = "renderEntitiesFix")
    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = CHUNK_GET_ENTITY_LISTS), require = 1)
    public ClassInheritanceMultiMap<Entity>[] getEntityList(Chunk chunk) {
        if (position == null) {
            return chunk.getEntityLists(); //TODO: is this right?
        }

        Cube cube = ((IColumn) chunk).getCube(Coords.blockToCube(position.getY()));
        if (cube instanceof BlankCube) {
            return ClassInheritanceMultiMapFactory.EMPTY_ARR;
        }

        return new ClassInheritanceMultiMap[]{cube.getEntityContainer().getEntitySet()};
    }

    /**
     * Overwrite getRenderChunk(For)Offset to support extended height.
     *
     * @author Barteks2x
     * @reason Remove hardcoded height checks, it's a simple method and doing it differently would be problematic and
     * confusing (Inject with local capture into BlockPos.getX() and redirect of BlockPos.getY())
     */
    @Nullable
    @Overwrite
    private RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing) {
        BlockPos blockpos = renderChunkBase.getBlockPosOffset16(facing);
        return MathHelper.abs(playerPos.getX() - blockpos.getX()) > this.renderDistanceChunks * 16 ? null :
                MathHelper.abs(playerPos.getY() - blockpos.getY()) > this.renderDistanceChunks * 16 ? null :
                        MathHelper.abs(playerPos.getZ() - blockpos.getZ()) > this.renderDistanceChunks * 16 ? null :
                                this.viewFrustum.getRenderChunk(blockpos);
    }

    /*
    @ModifyConstant(
            method = "renderWorldBorder",
            constant = {
                    @Constant(doubleValue = 0.0D),
                    @Constant(doubleValue = 256.0D)
            },
            slice = @Slice(from = @At(value = "HEAD"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V")), require = 2)
    private double renderWorldBorder_getRenderHeight(double original, Entity entity, float partialTicks) {
        return original == 0.0D ? entity.posY - 128 : entity.posY + 128;
    }
    */

    /**
     * we change this method a lot, because the border is rendered very differently in our narrow world.
     *
     * @author DaPorkchop_
     */
    @Inject(method = "renderWorldBorder",
            at = @At("HEAD"),
            cancellable = true)
    public void preRenderWorldBorder(Entity entity, float partialTicks, CallbackInfo callbackInfo) {
        if (PorkMethods.isForgeSever) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            WorldBorder worldborder = this.world.getWorldBorder();
            double d3 = (double) (this.mc.gameSettings.renderDistanceChunks * 16);

            if (true) {
                double d4 = 1.0D - worldborder.getClosestDistance(entity) / d3;
                d4 = Math.pow(d4, 4.0D);
                double d5 = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * (double) partialTicks;
                double d6 = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * (double) partialTicks;
                double d7 = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * (double) partialTicks;
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                this.renderEngine.bindTexture(FORCEFIELD_TEXTURES);
                GlStateManager.depthMask(false);
                GlStateManager.pushMatrix();
                int k1 = worldborder.getStatus().getColor();
                float f = (float) (k1 >> 16 & 255) / 255.0F;
                float f1 = (float) (k1 >> 8 & 255) / 255.0F;
                float f2 = (float) (k1 & 255) / 255.0F;
                GlStateManager.color(f, f1, f2, (float) d4);
                GlStateManager.doPolygonOffset(-3.0F, -3.0F);
                GlStateManager.enablePolygonOffset();
                GlStateManager.alphaFunc(516, 0.1F);
                GlStateManager.enableAlpha();
                GlStateManager.disableCull();
                float f3 = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F;
                bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
                bufferbuilder.setTranslation(-d5, -d6, -d7);
                double d8 = Math.max((double) MathHelper.floor(d7 - d3), worldborder.minZ());
                double d9 = Math.min((double) MathHelper.ceil(d7 + d3), worldborder.maxZ());

                if (d5 > worldborder.maxX() - d3) {
                    float f7 = 0.0F;

                    for (double d10 = d8; d10 < d9; f7 += 0.5F) {
                        double d11 = Math.min(1.0D, d9 - d10);
                        float f8 = (float) d11 * 0.5F;
                        bufferbuilder.pos(worldborder.maxX(), entity.posY + 128, d10).tex((double) (f3 + f7), (double) (f3 + 0.0F)).endVertex();
                        bufferbuilder.pos(worldborder.maxX(), entity.posY + 128, d10 + d11).tex((double) (f3 + f8 + f7), (double) (f3 + 0.0F)).endVertex();
                        bufferbuilder.pos(worldborder.maxX(), entity.posY - 128, d10 + d11).tex((double) (f3 + f8 + f7), (double) (f3 + 128.0F)).endVertex();
                        bufferbuilder.pos(worldborder.maxX(), entity.posY - 128, d10).tex((double) (f3 + f7), (double) (f3 + 128.0F)).endVertex();
                        ++d10;
                    }
                }

                if (d5 < worldborder.minX() + d3) {
                    float f9 = 0.0F;

                    for (double d12 = d8; d12 < d9; f9 += 0.5F) {
                        double d15 = Math.min(1.0D, d9 - d12);
                        float f12 = (float) d15 * 0.5F;
                        bufferbuilder.pos(worldborder.minX(), entity.posY + 128, d12).tex((double) (f3 + f9), (double) (f3 + 0.0F)).endVertex();
                        bufferbuilder.pos(worldborder.minX(), entity.posY + 128, d12 + d15).tex((double) (f3 + f12 + f9), (double) (f3 + 0.0F)).endVertex();
                        bufferbuilder.pos(worldborder.minX(), entity.posY - 128, d12 + d15).tex((double) (f3 + f12 + f9), (double) (f3 + 128.0F)).endVertex();
                        bufferbuilder.pos(worldborder.minX(), entity.posY - 128, d12).tex((double) (f3 + f9), (double) (f3 + 128.0F)).endVertex();
                        ++d12;
                    }
                }

                d8 = Math.max((double) MathHelper.floor(d5 - d3), worldborder.minX());
                d9 = Math.min((double) MathHelper.ceil(d5 + d3), worldborder.maxX());

                if (true) {
                    float f10 = 0.0F;

                    for (double d13 = d8; d13 < d9; f10 += 0.5F) {
                        double d16 = Math.min(1.0D, d9 - d13);
                        float f13 = (float) d16 * 0.5F;
                        bufferbuilder.pos(d13, entity.posY + 128, 256).tex((double) (f3 + f10), (double) (f3 + 0.0F)).endVertex();
                        bufferbuilder.pos(d13 + d16, entity.posY + 128, 256).tex((double) (f3 + f13 + f10), (double) (f3 + 0.0F)).endVertex();
                        bufferbuilder.pos(d13 + d16, entity.posY - 128, 256).tex((double) (f3 + f13 + f10), (double) (f3 + 128.0F)).endVertex();
                        bufferbuilder.pos(d13, entity.posY - 128, 256).tex((double) (f3 + f10), (double) (f3 + 128.0F)).endVertex();
                        ++d13;
                    }
                }

                if (true) {
                    float f11 = 0.0F;

                    for (double d14 = d8; d14 < d9; f11 += 0.5F) {
                        double d17 = Math.min(1.0D, d9 - d14);
                        float f14 = (float) d17 * 0.5F;
                        bufferbuilder.pos(d14, entity.posY + 128, 0).tex((double) (f3 + f11), (double) (f3 + 0.0F)).endVertex();
                        bufferbuilder.pos(d14 + d17, entity.posY + 128, 0).tex((double) (f3 + f14 + f11), (double) (f3 + 0.0F)).endVertex();
                        bufferbuilder.pos(d14 + d17, entity.posY - 128, 0).tex((double) (f3 + f14 + f11), (double) (f3 + 128.0F)).endVertex();
                        bufferbuilder.pos(d14, entity.posY - 128, 0).tex((double) (f3 + f11), (double) (f3 + 128.0F)).endVertex();
                        ++d14;
                    }
                }

                tessellator.draw();
                bufferbuilder.setTranslation(0.0D, 0.0D, 0.0D);
                GlStateManager.enableCull();
                GlStateManager.disableAlpha();
                GlStateManager.doPolygonOffset(0.0F, 0.0F);
                GlStateManager.disablePolygonOffset();
                GlStateManager.enableAlpha();
                GlStateManager.disableBlend();
                GlStateManager.popMatrix();
                GlStateManager.depthMask(true);
            }

            callbackInfo.cancel();
        }
    }
}
