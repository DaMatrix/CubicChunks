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
package cubicchunks.asm.mixin.fixes.common.worldgen;

import cubicchunks.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Random;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(WorldGenDungeons.class)
public abstract class MixinWorldGenDungeons {

    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    protected abstract ResourceLocation pickMobSpawner(Random rand);

    @ModifyConstant(method = "generate", constant = @Constant(
            intValue = 0,
            expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO,
            ordinal = 3))
    private int getMinHeight(int orig, World worldIn, Random rand, BlockPos position) {
        return ((ICubicWorld) worldIn).getMinHeight();
    }

    @Redirect(method = "generate",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/gen/feature/WorldGenDungeons;pickMobSpawner(Ljava/util/Random;)Lnet/minecraft/util/ResourceLocation;"))
    public ResourceLocation betterSpawnerPick(WorldGenDungeons dungeons, Random random, World worldIn, Random rand, BlockPos position) {
        if (position.getY() < -2000 && random.nextDouble() < 0.05d)    {
            return EntityList.getKey(EntityGuardian.class);
        }

        return net.minecraftforge.common.DungeonHooks.getRandomDungeonMob(rand);
    }
}
