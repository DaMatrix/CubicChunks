package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Arrays;

/**
 * Makes the overworld not have sky light, and makes all brightness levels look the same.
 *
 * @author DaPorkchop_
 */
@Mixin(WorldProviderSurface.class)
public abstract class MixinWorldProviderSurface extends WorldProvider {
    @Override
    protected void init() {
        super.init();
        this.hasSkyLight = false;
    }

    @Override
    protected void generateLightBrightnessTable() {
        Arrays.fill(this.lightBrightnessTable, (1.0F - (1.0F - 15.0f / 15.0F)) / ((1.0F - 15.0f / 15.0F) * 3.0F + 1.0F) * 1.0F + 0.0F);
    }
}
