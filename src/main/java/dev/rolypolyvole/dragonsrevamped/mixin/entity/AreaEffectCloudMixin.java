package dev.rolypolyvole.dragonsrevamped.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.rolypolyvole.dragonsrevamped.config.ModConfig;
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudMixin extends Entity {

    @Shadow
    public abstract LivingEntity getOwner();

    @Shadow
    public abstract float getRadius();

    protected AreaEffectCloudMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @ModifyExpressionValue(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<LivingEntity> filterAndBuffEndermen(List<LivingEntity> entities) {
        if (!(getOwner() instanceof EnderDragon)) {
            return entities;
        }

        float radius = getRadius();
        List<LivingEntity> filtered = new ArrayList<>();

        for (LivingEntity entity : entities) {
            if (entity instanceof CrystalProtector.CrystalProtectorHorse) continue;
            if (entity instanceof CrystalProtector) continue;

            if (entity instanceof Enderman enderman) {
                double dx = enderman.getX() - getX();
                double dz = enderman.getZ() - getZ();
                if (dx * dx + dz * dz <= radius * radius) {
                    enderman.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 1));
                    enderman.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
                }
            } else {
                filtered.add(entity);
            }
        }

        return filtered;
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void corrodeItems(ServerLevel serverLevel, CallbackInfo ci) {
        if (!(getOwner() instanceof EnderDragon) || !ModConfig.getCurrent().getMisc().getDragonBreathErodesItems()) return;

        float radius = getRadius();
        AABB aabb = getBoundingBox().inflate(radius, 2.0, radius);
        List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, aabb);

        for (ItemEntity item : items) {
            var stack = item.getItem();
            if (stack.is(Items.DRAGON_EGG)) continue;
            if (stack.has(DataComponents.DAMAGE_RESISTANT)) continue;

            double dx = item.getX() - getX();
            double dz = item.getZ() - getZ();
            if (dx * dx + dz * dz <= radius * radius) {
                item.discard();
            }
        }
    }
}
