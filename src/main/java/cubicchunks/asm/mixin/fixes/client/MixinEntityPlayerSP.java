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
package cubicchunks.asm.mixin.fixes.client;

import com.mojang.authlib.GameProfile;
import cubicchunks.asm.JvmNames;
import cubicchunks.asm.MixinUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.pepsi.ccaddon.PorkMethods;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(EntityPlayerSP.class)
public class MixinEntityPlayerSP extends AbstractClientPlayer {

    public MixinEntityPlayerSP(World worldIn, GameProfile playerProfile) {
        super(worldIn, playerProfile);
    }

    @ModifyConstant(method = "onUpdate", constant = @Constant(doubleValue = 0.0D))
    public double replaceEntityYForBlockPos(double value) {
        return this.posY;
    }

    @Redirect(method = "onUpdate", at = @At(target = JvmNames.WORLD_IS_BLOCK_LOADED, value = "INVOKE"))
    public boolean canEntityUpdate_isBlockLoadedRedirect(World world, BlockPos pos) {
        return MixinUtils.canTickPosition(world, pos);
    }

    @Inject(method = "Lnet/minecraft/client/entity/EntityPlayerSP;setServerBrand(Ljava/lang/String;)V",
    at = @At("HEAD"))
    public void fixBorder(String brand, CallbackInfo callbackInfo)  {
        PorkMethods.isForgeSever = brand.trim().toLowerCase().endsWith("forge");
    }
}
