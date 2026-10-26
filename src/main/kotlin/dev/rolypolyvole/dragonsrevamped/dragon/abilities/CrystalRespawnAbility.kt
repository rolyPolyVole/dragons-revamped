package dev.rolypolyvole.dragonsrevamped.dragon.abilities

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.dragon.Crystals
import dev.rolypolyvole.dragonsrevamped.dragon.DragonAbilityManager
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector
import dev.rolypolyvole.dragonsrevamped.util.Text
import dev.rolypolyvole.dragonsrevamped.util.broadcastMessage
import dev.rolypolyvole.dragonsrevamped.worldgen.CustomEndSpikes
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

class CrystalRespawnAbility(
    dragon: EnderDragon,
    manager: DragonAbilityManager
) : AbstractDragonAbility(dragon, manager) {

    private val level = dragon.level() as ServerLevel

    /** Crystal location to remaining per-location cooldown ticks **/
    private val crystalLocations = CustomEndSpikes.getCrystalLocations(level)
        .associateWith { 0 }
        .toMutableMap()

    /** Global cooldown **/
    private var cooldown = calculateCooldown()
    private var tooManyCrystals = false

    private fun calculateCooldown(): Int {
        val config = ModConfig.current
        val crystals = getTotalCrystals()

        val min = 400
        val scaled = 1200 / sqrt(config.fighters.toDouble())
        val extra = crystals * 150

        return ((min + scaled + extra) / config.crystals.respawnRate).toInt()
    }

    override fun tick() {
        crystalLocations.replaceAll { _, value ->
            (value - 1).coerceAtLeast(0)
        }

        if (dragon.tickCount % CRYSTAL_CHECK_INTERVAL == 0) {
            tooManyCrystals = getTotalCrystals() >= ModConfig.current.crystals.respawnThreshold
        }

        if (tooManyCrystals) return

        if (cooldown > 0) {
            cooldown--
        } else if (respawnCrystal()) {
            cooldown = calculateCooldown()
        }
    }

    private fun respawnCrystal(): Boolean {
        val pos = crystalLocations
            .filter { it.value == 0 }.keys
            .filterNot(::doesCrystalExist)
            .randomOrNull() ?: return false

        val crystal = Crystals.respawn(level, pos) ?: return false
        crystal.playSound(SoundEvents.BEACON_ACTIVATE)

        dragon.broadcastMessage(Text.parse("<light_purple>An End Crystal has respawned!"))
        sendCrystalCount()
        return true
    }

    fun doesCrystalExist(vec: Vec3): Boolean {
        val aabb = AABB(vec.add(-0.5, -0.5, -0.5), vec.add(0.5, 0.5, 0.5)).inflate(1.5)
        return level.getEntitiesOfClass(EndCrystal::class.java, aabb).isNotEmpty()
    }

    fun onCrystalDestroyed(pos: BlockPos) {
        val key = crystalLocations.keys.minByOrNull { it.distanceToSqr(Vec3.atCenterOf(pos)) } ?: return
        crystalLocations[key] = 600
    }

    fun resetCooldowns() {
        crystalLocations.replaceAll { _, _ -> 0 }
    }

    fun getTotalCrystals(): Int {
        val pillarCrystals = crystalLocations.keys.count(::doesCrystalExist)
        val protectorCrystals = getProtectorCrystals()
        return pillarCrystals + protectorCrystals
    }

    private fun getProtectorCrystals(): Int {
        val center = Vec3.atCenterOf(dragon.fightOrigin)
        val aabb = AABB.ofSize(center, 400.0, 400.0, 400.0)

        return level
            .getEntitiesOfClass(CrystalProtector::class.java, aabb)
            .count(CrystalProtector::hasCrystal)
    }

    fun sendCrystalCount() {
        val count = getTotalCrystals()
        val message = Text.parse("<dark_purple>✦ Crystals Remaining: $count")

        dragon.broadcastMessage(message, true)
    }

    private companion object {
        const val CRYSTAL_CHECK_INTERVAL = 20
    }
}
