package dev.rolypolyvole.dragonsrevamped.mixin.dragon;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector;
import dev.rolypolyvole.dragonsrevamped.util.Text;
import net.kyori.adventure.text.ComponentLike;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EndCrystal.class)
public abstract class EndCrystalMixin {

    @Unique
    private EndCrystal self() {
        return (EndCrystal)(Object) this;
    }

    @ModifyExpressionValue(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BaseFireBlock;getState(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState useSoulFire(BlockState original) {
        return Blocks.SOUL_FIRE.defaultBlockState();
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void reloadCrystalProtectors(CallbackInfo ci) {
        EndCrystal self = self();
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        for (Entity passenger : self.getPassengers()) {
            if (passenger instanceof Skeleton && !(passenger instanceof CrystalProtector)) {
                passenger.discard();

                CrystalProtector protector = new CrystalProtector(serverLevel);
                protector.equipArmor();
                protector.startRiding(self, true, false);
                serverLevel.addFreshEntity(protector);
            }
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void preventDamageIfGuarded(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        EndCrystal self = self();

        if (self instanceof CrystalProtector.OrbitCrystal) return;

        List<CrystalProtector> protectors = level.getEntitiesOfClass(
            CrystalProtector.class,
            self.getBoundingBox().inflate(12.0)
        );

        boolean guarded = protectors.stream().anyMatch(Entity::isAlive);

        if (guarded) {
            Entity attacker = source.getEntity();

            if (attacker instanceof ServerPlayer player) {
                player.sendActionBar((ComponentLike) Text.parse(
                    "<light_purple>This crystal is guarded by a nearby Crystal Protector!"
                ));
            }

            cir.setReturnValue(false);
        }
    }
}
