package dev.rolypolyvole.dragonsrevamped.mixin.worldgen;

import dev.rolypolyvole.dragonsrevamped.worldgen.CustomEndSpikes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndSpikeFeature.class)
public abstract class EndSpikeFeatureMixin {

    @Inject(method = "placeSpike", at = @At("HEAD"), cancellable = true)
    private void buildCustomEndSpike(ServerLevelAccessor level, RandomSource random, EndSpikeFeature.EndSpike spike, CallbackInfo ci) {
        CustomEndSpikes.place(level, (EndSpikeFeature)(Object) this, spike);
        ci.cancel();
    }
}
