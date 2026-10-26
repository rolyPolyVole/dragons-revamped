package dev.rolypolyvole.dragonsrevamped.mixin.entity;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @WrapWithCondition(method = "stabAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;stopRiding()V"))
    private boolean keepCrystalProtectorsMounted(Entity target) {
        return !(target instanceof CrystalProtector);
    }
}
