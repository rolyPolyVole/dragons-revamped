package dev.rolypolyvole.dragonsrevamped.mixin;

import com.mojang.authlib.GameProfile;
import dev.rolypolyvole.dragonsrevamped.data.DragonDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
@SuppressWarnings("NameDoesntMatchTargetClass")
public abstract class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Shadow
    public abstract @NonNull ServerLevel level();

    @Unique
    private DamageSource deathSource;

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void saveDeathSource(DamageSource damageSource, CallbackInfo ci) {
        this.deathSource = damageSource;
    }

    @Redirect(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;getDeathMessage()Lnet/minecraft/network/chat/Component;"))
    private Component customDeathMessage(CombatTracker tracker) {
        Component value = tracker.getDeathMessage();

        if (deathSource == null || deathSource.getDirectEntity() == null) {
            return value;
        }

        var name = deathSource.getDirectEntity().getDisplayName();

        if (DragonDamageTypes.INSTANCE.dragonLightning(level()) == deathSource.typeHolder()) {
            return Component.empty()
                .append(getDisplayName())
                .append(" was struck down by ")
                .append(name);
        }

        if (DragonDamageTypes.INSTANCE.dragonBodyHit(level()) == deathSource.typeHolder()) {
            return Component.empty()
                .append(getDisplayName())
                .append(" got ran through by ")
                .append(name);
        }

        return value;
    }
}
