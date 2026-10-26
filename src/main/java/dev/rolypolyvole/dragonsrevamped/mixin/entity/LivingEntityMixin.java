package dev.rolypolyvole.dragonsrevamped.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.rolypolyvole.dragonsrevamped.config.ModConfig;
import dev.rolypolyvole.dragonsrevamped.data.DragonDamageTypes;
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private static final float DRAGON_DURABILITY_DAMAGE = 0.25F;

    @WrapWithCondition(method = "stabAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;stopRiding()V"))
    private boolean keepCrystalProtectorsMounted(Entity target) {
        return !(target instanceof CrystalProtector);
    }

    @SuppressWarnings("LocalMayUseName")
    @ModifyExpressionValue(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getDamageProtection(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;)F"))
    private float weakenProtectionAgainstBodyHits(float protection, @Local(argsOnly = true) DamageSource source) {
        return source.is(DragonDamageTypes.DRAGON_BODY_HIT) ? protection * (float) ModConfig.getCurrent().getDamage().getProtectionEffectiveness() : protection;
    }

    @WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtArmor(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void reduceDragonDurabilityDamage(LivingEntity entity, DamageSource damageSource, float damage, Operation<Void> original) {
        Entity attacker = damageSource.getEntity();
        boolean fromDragonFight = attacker instanceof EnderDragon || attacker instanceof CrystalProtector;
        float durabilityDamage = fromDragonFight ? damage * DRAGON_DURABILITY_DAMAGE : damage;
        original.call(entity, damageSource, durabilityDamage);
    }
}
