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
package cubicchunks.asm.mixin.fixes.server;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.play.client.CPacketVehicleMove;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {
    @Shadow
    public EntityPlayerMP player;

    @Inject(method = "Lnet/minecraft/network/NetHandlerPlayServer;processVehicleMove(Lnet/minecraft/network/play/client/CPacketVehicleMove;)V",
            at = @At(value = "INVOKE",
                    target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
                    ordinal = 0))
    public void dismountFastVehicles(CPacketVehicleMove move, CallbackInfo callbackInfo) {
        player.dismountRidingEntity();
    }

    @ModifyConstant(method = "Lnet/minecraft/network/NetHandlerPlayServer;processVehicleMove(Lnet/minecraft/network/play/client/CPacketVehicleMove;)V",
            constant = @Constant(
                    doubleValue = 100.0D
            ))
    public double preventHighVehicleSpeed(double in) {
        return 50.0D;
    }

    @ModifyConstant(method = "Lnet/minecraft/network/NetHandlerPlayServer;processPlayer(Lnet/minecraft/network/play/client/CPacketPlayer;)V",
            constant = @Constant(
                    floatValue = 100.0f
            ))
    public float preventHighPlayerSpeeds1(float old) {
        return 50.0f;
    }

    @ModifyConstant(method = "Lnet/minecraft/network/NetHandlerPlayServer;processPlayer(Lnet/minecraft/network/play/client/CPacketPlayer;)V",
            constant = @Constant(
                    floatValue = 300.0f
            ))
    public float preventHighPlayerSpeeds2(float old) {
        return 150.0f;
    }
}
