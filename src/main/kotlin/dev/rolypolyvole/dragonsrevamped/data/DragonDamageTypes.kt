package dev.rolypolyvole.dragonsrevamped.data

import dev.rolypolyvole.dragonsrevamped.DragonsRevamped
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.boss.enderdragon.EnderDragon

object DragonDamageTypes {
    @JvmField
    val DRAGON_LIGHTNING: ResourceKey<DamageType> = create("dragon_lightning")

    @JvmField
    val DRAGON_BODY_HIT: ResourceKey<DamageType> = create("dragon_body_hit")

    @JvmStatic
    fun dragonLightning(level: ServerLevel, dragon: EnderDragon): DamageSource = source(level, DRAGON_LIGHTNING, dragon)

    @JvmStatic
    fun dragonBodyHit(level: ServerLevel, dragon: EnderDragon): DamageSource = source(level, DRAGON_BODY_HIT, dragon)

    private fun create(name: String): ResourceKey<DamageType> =
        ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(DragonsRevamped.MOD_ID, name))

    private fun source(level: ServerLevel, type: ResourceKey<DamageType>, dragon: EnderDragon): DamageSource {
        val holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type)
        return DamageSource(holder, dragon)
    }
}
