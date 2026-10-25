package dev.rolypolyvole.dragonsrevamped.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import dev.rolypolyvole.dragonsrevamped.data.DragonDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerPlayer.class)
@SuppressWarnings("NameDoesntMatchTargetClass")
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @ModifyExpressionValue(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"))
    private Component customDeathMessage(Component original, @Local(argsOnly = true) DamageSource damageSource) {
        Entity attacker = damageSource.getDirectEntity();

        if (attacker == null) {
            return original;
        }

        if (damageSource.is(DragonDamageTypes.DRAGON_LIGHTNING)) {
            return Component.empty()
                .append(getDisplayName())
                .append(" was struck down by ")
                .append(attacker.getDisplayName());
        }

        if (damageSource.is(DragonDamageTypes.DRAGON_BODY_HIT)) {
            return Component.empty()
                .append(getDisplayName())
                .append(" got ran through by ")
                .append(attacker.getDisplayName());
        }

        return original;
    }
}
