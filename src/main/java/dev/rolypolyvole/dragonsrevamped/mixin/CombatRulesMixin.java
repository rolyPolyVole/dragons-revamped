package dev.rolypolyvole.dragonsrevamped.mixin;

import dev.rolypolyvole.dragonsrevamped.data.DragonDamageTypes;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CombatRules.class)
@SuppressWarnings("NameDoesntMatchTargetClass")
public class CombatRulesMixin {

    @Unique
    private static final float ARMOR_BREACH = 0.45F;

    @Inject(method = "getDamageAfterAbsorb", at = @At("RETURN"), cancellable = true)
    private static void bodyHitArmorBypass(LivingEntity entity, float damage, DamageSource source, float armor, float armorToughness, CallbackInfoReturnable<Float> cir) {
        if (!source.is(DragonDamageTypes.INSTANCE.getDRAGON_BODY_HIT())) return;

        float absorbed = damage - cir.getReturnValueF();
        cir.setReturnValue(damage - absorbed * (1.0F - ARMOR_BREACH));
    }
}
