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

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.FMLLog;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.pepsi.ccaddon.PorkMethods;

import static cubicchunks.asm.JvmNames.CHUNK_IS_POPULATED;

/**
 * Currently only fixes markAndNotifyBlock checking if chunk is populated instead of checking cubes.
 */
@Mixin(World.class)
public abstract class MixinWorld implements ICubicWorld {

    @Shadow
    @Final
    public WorldProvider provider;
    public BlockPos addon_forcedSpawn = new BlockPos(0, PorkMethods.overworldSpawnOffset, 128);

    @Shadow
    public abstract WorldBorder getWorldBorder();

    // note: markAndNotifyBlock has @Nullable on chunk, this will never be null here,
    // because this isgit lo the chunk on which isPopulated is called
    @Redirect(method = "markAndNotifyBlock", at = @At(value = "INVOKE", target = CHUNK_IS_POPULATED))
    public boolean markNotifyBlock_CubeCheck(Chunk _this,
                                             BlockPos pos, Chunk chunk, IBlockState oldstate,
                                             IBlockState newState, int flags) {
        if (!this.isCubicWorld()) {
            // vanilla compatibility
            return chunk.isPopulated();
        }
        IColumn IColumn = (IColumn) chunk;
        Cube cube = IColumn.getCube(Coords.blockToCube(pos.getY()));
        return cube.isFullyPopulated();
    }

    @Inject(method = "Lnet/minecraft/world/World;getBiome(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/biome/Biome;", at = @At("HEAD"), cancellable = true)
    public void checkEnd(BlockPos pos, CallbackInfoReturnable<Biome> callbackInfoReturnable)    {
        if (pos.y > 14000)  {
            callbackInfoReturnable.setReturnValue(Biomes.SKY);
        }
    }
    @Inject(method = "getSpawnPoint",
            at = @At("HEAD"),
            cancellable = true)
    public void preGetSpawnPoint(CallbackInfoReturnable<BlockPos> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(addon_forcedSpawn);
    }

    /*@Inject(method = "Lnet/minecraft/world/World;updateEntityWithOptionalForce(Lnet/minecraft/entity/Entity;Z)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/profiler/Profiler;endSection()V"))
    public void betterMovementCheck(Entity e, boolean idk, CallbackInfo callbackInfo) {
        //if (e.is)
        double diffX = Math.abs(e.posX - e.lastTickPosX);
        double diffY = Math.abs(e.posY - e.lastTickPosY);
        double diffZ = Math.abs(e.posZ - e.lastTickPosZ);
        if (diffX * diffX + diffY * diffY + diffZ * diffZ >= 4) { //2 blocks in one tick
            e.posX = e.lastTickPosX;
            e.posY = e.lastTickPosY;
            e.posZ = e.lastTickPosZ;
            FMLLog.info("Entity " + e.getName() + " moved too fast xd");
        }
    }*/
}
