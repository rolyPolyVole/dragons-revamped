package dev.rolypolyvole.dragonsrevamped.mixin.dragon;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerEntityMixin {

    @Shadow @Final
    private Entity entity;

    @Inject(method = "sendChanges", at = @At("HEAD"))
    private void preventInsaneWingFlaps(CallbackInfo ci) {
        if (!(entity instanceof EnderDragon dragon)) return;
        if (dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.HOVERING) return;

        entity.needsSync = false;
        entity.syncVelocity = false;
    }
}
