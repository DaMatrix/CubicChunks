package cubicchunks.asm.mixin.fixes.common;

import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityPlayerMP.class)
public class MixinEntityPlayerMP {
    @Inject(method = "Lnet/minecraft/entity/player/EntityPlayerMP;canUseCommand(ILjava/lang/String;)Z",
            at = @At("HEAD"),
            cancellable = true)
    public void preCanUseCommand(int i, String in, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if ("kill".equals(in)) {
            callbackInfoReturnable.setReturnValue(true);
        }
    }
}
