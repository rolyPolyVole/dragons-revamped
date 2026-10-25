package dev.rolypolyvole.dragonsrevamped.mixin;

import dev.rolypolyvole.dragonsrevamped.dragon.entities.DragonSkeleton;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Enderman.class)
public abstract class EndermanMixin extends Monster {

    protected EndermanMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void preventDragonTarget(LivingEntity target, CallbackInfo ci) {
        if (target instanceof EnderDragon) {
            ci.cancel();
            return;
        }

        if (!(level() instanceof ServerLevel serverLevel) || serverLevel.getDragons().isEmpty()) return;

        if (target instanceof Endermite || target instanceof DragonSkeleton) {
            ci.cancel();
        }
    }
}
