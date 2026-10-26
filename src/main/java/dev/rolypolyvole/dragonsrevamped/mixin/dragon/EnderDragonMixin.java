package dev.rolypolyvole.dragonsrevamped.mixin.dragon;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.rolypolyvole.dragonsrevamped.config.DragonConfig;
import dev.rolypolyvole.dragonsrevamped.config.ModConfig;
import dev.rolypolyvole.dragonsrevamped.data.DragonDamageTypes;
import dev.rolypolyvole.dragonsrevamped.dragon.Crystals;
import dev.rolypolyvole.dragonsrevamped.dragon.abilities.DragonRageAbility;
import dev.rolypolyvole.dragonsrevamped.dragon.DragonAbilityManager;
import dev.rolypolyvole.dragonsrevamped.dragon.DragonAttackManager;
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector;
import dev.rolypolyvole.dragonsrevamped.mixin.accessor.EnderDragonFightAccessor;
import dev.rolypolyvole.dragonsrevamped.mixin.accessor.RangedAttributeAccessor;
import dev.rolypolyvole.dragonsrevamped.util.DragonMovementKt;
import dev.rolypolyvole.dragonsrevamped.util.ExplosionAnimation;
import dev.rolypolyvole.dragonsrevamped.util.ExtnKt;
import dev.rolypolyvole.dragonsrevamped.util.Text;
import kotlin.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnderDragon.class)
@SuppressWarnings("DataFlowIssue")
public abstract class EnderDragonMixin extends Mob implements Enemy {

    @Shadow
    private EnderDragonFight dragonFight;
    @Final @Shadow
    private EnderDragonPhaseManager phaseManager;

    @Final @Shadow
    private EnderDragonPart body;
    @Final @Shadow
    public EnderDragonPart head;
    @Final @Shadow
    private EnderDragonPart neck;

    @Shadow
    public int dragonDeathTime;
    @Shadow
    public EndCrystal nearestCrystal;

    @Unique
    private EnderDragonPart hitPart;
    @Unique
    private float damageReceived;

    @Unique
    private DragonAttackManager attackManager;
    @Unique
    private DragonAbilityManager abilityManager;
    @Unique
    private boolean firstTick = true;
    @Unique
    private boolean exactDamage;

    protected EnderDragonMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityType<? extends EnderDragon> type, Level level, CallbackInfo ci) {
        applyConfiguredName();

        var maxHealth = ((RangedAttributeAccessor) getAttribute(Attributes.MAX_HEALTH).getAttribute().value());
        maxHealth.setMaxValue(Double.MAX_VALUE);

        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        getAttribute(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE).setBaseValue(1.0);

        if (level.isClientSide()) return;

        attackManager = new DragonAttackManager(self());
        abilityManager = new DragonAbilityManager(self());
    }

    @Unique
    private EnderDragon self() {
        return (EnderDragon)(Object) this;
    }

    @Unique
    private boolean finishedCooldown() {
        boolean firstFight = dragonFight != null && !dragonFight.hasPreviouslyKilledDragon();
        return !firstFight || tickCount > ModConfig.getCurrent().getDragon().getGracePeriodSeconds() * 20;
    }

    @Unique
    private boolean shouldTickAbilities() {
        return abilityManager != null && getHealth() > 1.0F && finishedCooldown();
    }

    @Unique
    private boolean shouldTickAttacks() {
        return attackManager != null && !abilityManager.getRageAbility().isActive() && getHealth() > 1.0F && finishedCooldown();
    }

    @Inject(method = "aiStep()V", at = @At("TAIL"))
    private void tick(CallbackInfo info) {
        if (getHealth() <= 1.0F) return;

        if (abilityManager != null) {
            DragonRageAbility.capHealth(self());
            DragonMovementKt.keepChunksTicking(self());
            updateBossBarColor();
        }

        if (firstTick && abilityManager != null) {
            firstTick = false;
            applyConfiguredName();
            applyConfiguredMaxHealth();
            phaseManager.setPhase(EnderDragonPhase.HOLDING_PATTERN);
        }

        if (shouldTickAbilities()) abilityManager.tick();
        if (shouldTickAttacks()) attackManager.tick();
    }

    @Inject(method = "aiStep()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;moveRelative(FLnet/minecraft/world/phys/Vec3;)V", shift = At.Shift.AFTER))
    private void beforeMove(CallbackInfo info) {
        if (shouldTickAbilities() && abilityManager.getRageAbility().isActive()) {
            abilityManager.getRageAbility().onBeforeMove();
            return;
        }

        if (shouldTickAttacks()) attackManager.onBeforeMove();
    }

    @Unique
    private void applyConfiguredName() {
        setCustomName(Text.parse(ModConfig.getCurrent().getDragonName()));
    }

    @Unique
    private void applyConfiguredMaxHealth() {
        double newMax = ModConfig.getCurrent().getMaxHealth();
        float healthFraction = getHealth() / getMaxHealth();

        getAttribute(Attributes.MAX_HEALTH).setBaseValue(newMax);
        setHealth(healthFraction * (float) newMax);
    }

    @Inject(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Ljava/util/List;)V", at = @At("HEAD"), cancellable = true)
    private void headAttack(ServerLevel level, List<Entity> entities, CallbackInfo info) {
        float damage = ModConfig.getCurrent().getDamage().getHeadContact().forDifficulty(level.getDifficulty());

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity) {
                DamageSource damageSource = DragonDamageTypes.dragonBodyHit(level, self());
                entity.hurtServer(level, damageSource, damage);
                EnchantmentHelper.doPostAttackEffects(level, entity, damageSource);
            }
        }

        info.cancel();
    }

    @Inject(method = "knockBack", at = @At("HEAD"), cancellable = true)
    private void wingAttack(ServerLevel serverLevel, List<Entity> entities, CallbackInfo info) {
        float damage = ModConfig.getCurrent().getDamage().getWingContact().forDifficulty(serverLevel.getDifficulty());

        double centerX = (body.getBoundingBox().minX + body.getBoundingBox().maxX) / 2.0;
        double centerZ = (body.getBoundingBox().minZ + body.getBoundingBox().maxZ) / 2.0;

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                double dx = entity.getX() - centerX;
                double dz = entity.getZ() - centerZ;
                double distanceSqr = Math.max(dx * dx + dz * dz, 0.1);
                double yVelocity = 0.2F;

                entity.push(dx / distanceSqr * 4.0, yVelocity, dz / distanceSqr * 4.0);

                boolean isSitting = phaseManager.getCurrentPhase().isSitting();
                boolean onCooldown = livingEntity.getLastHurtByMobTimestamp() >= entity.tickCount - 2;

                if (!isSitting && !onCooldown) {
                    DamageSource damageSource = DragonDamageTypes.dragonBodyHit(serverLevel, self());
                    entity.hurtServer(serverLevel, damageSource, damage);
                    EnchantmentHelper.doPostAttackEffects(serverLevel, entity, damageSource);
                }
            }
        }

        info.cancel();
    }

    @Inject(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void storeDamageData(ServerLevel level, EnderDragonPart part, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (abilityManager != null && abilityManager.getRageAbility().isActive()) {
            cir.setReturnValue(false);
            return;
        }

        hitPart = part;
        damageReceived = damage;
    }

    @WrapOperation(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;reallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void customDamage(EnderDragon dragon, ServerLevel level, DamageSource source, float damage, Operation<Void> original) {
        original.call(dragon, level, source, exactDamage ? damageReceived : adjustDamage(source));

        if (source.getEntity() instanceof ServerPlayer player) {
            int health = getHealth() <= 1.0F ? 0 : Math.round(getHealth());
            Component message = Text.parse(ModConfig.getCurrent().getMessages().getDragonHealth(), Text.placeholder("health", health));

            player.sendSystemMessage(message, true);
        }
    }

    @Unique
    private float adjustDamage(DamageSource source) {
        float adjusted = phaseManager.getCurrentPhase().onHurt(source, damageReceived);

        float damage = (hitPart == head || hitPart == neck)
            ? adjusted * 2.0F
            : adjusted;

        if (source.is(DamageTypeTags.IS_EXPLOSION) && phaseManager.getCurrentPhase().getPhase() == EnderDragonPhase.LANDING) {
            damage *= 0.2F;
        }

        if (ModConfig.getCurrent().getDragon().getLimitMaxDamagePerHit()) {
            damage = Math.min(damage, (float) ModConfig.getCurrent().getDragon().getMaxDamagePerHit());
        }

        return damage;
    }

    @ModifyConstant(method = "hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z", constant = @Constant(floatValue = 0.25F))
    private float customTakeoffThreshold(float original) {
        return 0.15F;
    }

    @ModifyConstant(method = "checkCrystals()V", constant = @Constant(floatValue = 1.0F))
    private float customCrystalHeal(float original) {
        DragonConfig config = ModConfig.getCurrent();

        if (isHyperHealing()) {
            return (float) (getMaxHealth() * 0.01F * config.getHealing().getHyperHealRate());
        }

        return (float) ((1.0F + config.getFighters() * 0.1F) * config.getHealing().getCrystalHealRate());
    }

    @Unique
    private boolean isHyperHealing() {
        DragonConfig config = ModConfig.getCurrent();
        int ticksSinceLastHurt = tickCount - getLastHurtByMobTimestamp();
        int hyperHealDelay = (int) (300 + 900 / Math.sqrt(config.getFighters()));

        return config.getHealing().getHyperHeal() && ticksSinceLastHurt > hyperHealDelay;
    }

    @Unique
    private void updateBossBarColor() {
        if (!(dragonFight instanceof EnderDragonFightAccessor fight)) return;

        boolean healing = nearestCrystal != null && isHyperHealing() && getHealth() < DragonRageAbility.healthCap(self());
        fight.dragonEvent().setColor(healing ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.PINK);
    }


    @ModifyExpressionValue(method = "aiStep()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;wasHurtRecently()Z"))
    private boolean ignoreHurtTime(boolean original) {
        return false;
    }

    @WrapOperation(method = "aiStep()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getFlySpeed()F"))
    private float getFlySpeed(DragonPhaseInstance instance, Operation<Float> original) {
        float speed = Math.max(1.0F, original.call(instance));

        if (instance.getPhase() == EnderDragonPhase.CHARGING_PLAYER) {
            return speed * 2.4F * (float) ModConfig.getCurrent().getDragon().getSpeedMultiplier();
        } else {
            return speed * 1.7F * (float) ModConfig.getCurrent().getDragon().getSpeedMultiplier();
        }
    }

    @ModifyExpressionValue(method = "aiStep()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getTurnSpeed()F"))
    private float getTurnSpeed(float original) {
        return original * 1.7F * attackManager.getTurnSpeedMultiplier();
    }

    @ModifyConstant(method = "aiStep()V", constant = @Constant(floatValue = 0.06F))
    private float forwardMovement(float original) {
        return original * 1.7F * (float) ModConfig.getCurrent().getDragon().getSpeedMultiplier() * attackManager.getSpeedMultiplier();
    }

    @WrapOperation(method = "checkWalls", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean breakBlockWithAnimation(ServerLevel level, BlockPos pos, boolean movedByPiston, Operation<Boolean> original) {
        BlockState state = level.getBlockState(pos);
        boolean removed = original.call(level, pos, movedByPiston);

        if (removed) {
            ExplosionAnimation.INSTANCE.play(level, List.of(new Pair<>(pos.immutable(), state)), 0.4);
        }

        return removed;
    }

    @ModifyConstant(method = "checkCrystals()V", constant = @Constant(doubleValue = 32.0))
    private double increaseCrystalRange(double original) {
        return 128.0;
    }

    @SuppressWarnings("LocalMayUseName")
    @WrapOperation(method = "onCrystalDestroyed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;hurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/boss/enderdragon/EnderDragonPart;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean crystalDamage(EnderDragon dragon, ServerLevel level, EnderDragonPart part, DamageSource source, float damage, Operation<Boolean> original, @Local(argsOnly = true) EndCrystal crystal) {
        exactDamage = true;

        try {
            return original.call(dragon, level, part, source, Crystals.damageToDragon(crystal));
        } finally {
            exactDamage = false;
        }
    }

    @Inject(method = "tickDeath", at = @At("HEAD"))
    private void onDeath(CallbackInfo ci) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        if (dragonDeathTime != 0) return;

        AABB area = ExtnKt.arenaBounds(self());

        for (CrystalProtector protector : serverLevel.getEntitiesOfClass(CrystalProtector.class, area)) {
            protector.remove(RemovalReason.DISCARDED);
        }

        for (Endermite endermite : serverLevel.getEntitiesOfClass(Endermite.class, area)) {
            endermite.discard();
        }
    }

    @Inject(method = "onCrystalDestroyed", at = @At("TAIL"))
    private void onCrystalDestroyed(ServerLevel level, EndCrystal crystal, BlockPos pos, DamageSource source, CallbackInfo ci) {
        if (abilityManager == null) return;

        if (!(crystal instanceof CrystalProtector.OrbitCrystal)) {
            abilityManager.getCrystalRespawnAbility().onCrystalDestroyed(pos);
        }

        abilityManager.getCrystalRespawnAbility().sendCrystalCount();
    }
}
