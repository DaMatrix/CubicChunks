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
package cubicchunks.asm.mixin.core.common;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import cubicchunks.event.CCEventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Inject(method = "loadAllWorlds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/WorldSettings;setGeneratorOptions(Ljava/lang/String;)Lnet/minecraft/world/WorldSettings;",
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILHARD)
    private void onWorldSettingsCreate(String saveName, String worldNameIn, long seed, WorldType type, String generatorOptions, CallbackInfo cbi, ISaveHandler isavehandler, WorldInfo worldinfo, WorldSettings worldsettings) {
        CCEventFactory.onWorldSettingsCreate(worldsettings);
    }

}
