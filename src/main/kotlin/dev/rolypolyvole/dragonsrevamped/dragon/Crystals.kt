package dev.rolypolyvole.dragonsrevamped.dragon

import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.Vec3

object Crystals {
    private const val RESPAWNED_TAG = "dragons_revamped_respawned"
    private const val FRESH_CRYSTAL_DAMAGE_FRACTION = 0.065F
    private const val RESPAWNED_CRYSTAL_DAMAGE = 30.0F

    fun spawnRespawned(level: ServerLevel, position: Vec3): EndCrystal? {
        val crystal = EntityTypes.END_CRYSTAL.create(level, EntitySpawnReason.EVENT) ?: return null

        crystal.setPos(position)
        crystal.setShowBottom(false)
        crystal.addTag(RESPAWNED_TAG)
        level.addFreshEntity(crystal)

        return crystal
    }

    @JvmStatic
    fun damageToDragon(crystal: EndCrystal, dragonMaxHealth: Float): Float =
        if (isFresh(crystal)) dragonMaxHealth * FRESH_CRYSTAL_DAMAGE_FRACTION else RESPAWNED_CRYSTAL_DAMAGE

    private fun isFresh(crystal: EndCrystal): Boolean =
        crystal !is CrystalProtector.OrbitCrystal && RESPAWNED_TAG !in crystal.entityTags()
}
