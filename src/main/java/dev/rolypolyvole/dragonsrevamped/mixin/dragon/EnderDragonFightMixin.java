package dev.rolypolyvole.dragonsrevamped.mixin.dragon;

import dev.rolypolyvole.dragonsrevamped.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonFight.class)
public abstract class EnderDragonFightMixin {

    @Shadow
    private ServerBossEvent dragonEvent;

    @Inject(method = "init", at = @At("RETURN"))
    private void customizeBossBar(ServerLevel level, long seed, BlockPos origin, CallbackInfo ci) {
        if (ModConfig.getCurrent().getDisableFog()) {
            dragonEvent.setCreateWorldFog(false);
        }

        dragonEvent.setOverlay(BossEvent.BossBarOverlay.NOTCHED_6);
    }
}
