package dev.rolypolyvole.dragonsrevamped.dragon.abilities

import dev.rolypolyvole.dragonsrevamped.dragon.DragonAbilityManager
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector
import dev.rolypolyvole.dragonsrevamped.util.broadcastMessage
import dev.rolypolyvole.dragonsrevamped.util.nearbyPlayers
import dev.rolypolyvole.dragonsrevamped.worldgen.CustomEndSpikes
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

class CrystalRespawnAbility(
    dragon: EnderDragon,
    manager: DragonAbilityManager
) : AbstractDragonAbility(dragon, manager) {

    private val level = this.dragon.level() as ServerLevel

    private val crystalLocations = CustomEndSpikes.getCrystalLocations(this.level)
        .associateWith { 0 }
        .toMutableMap()

    private var cooldown = calculateCooldown()

    private fun calculateCooldown(): Int {
        val playerCount = this.dragon.nearbyPlayers().size.coerceAtLeast(1)
        val crystals = getTotalCrystals()

        val min = 400
        val scaled = (1200 / sqrt(playerCount.toDouble())).toInt()
        val extra = crystals * 150

        return min + scaled + extra
    }

    override fun tick() {
        this.crystalLocations.replaceAll { _, value ->
            (value - 1).coerceAtLeast(0)
        }

        if (this.cooldown > 0) {
            this.cooldown--
        } else {
            respawnCrystal()
            this.cooldown = calculateCooldown()
        }
    }

    private fun respawnCrystal() {
        val pos = this.crystalLocations
            .filter { it.value == 0 }.keys
            .filterNot(::doesCrystalExist)
            .randomOrNull() ?: return

        val crystal = EntityTypes.END_CRYSTAL.create(this.level, EntitySpawnReason.EVENT) ?: return

        crystal.setPos(pos)
        crystal.setShowBottom(false)
        crystal.playSound(SoundEvents.BEACON_ACTIVATE)

        this.level.addFreshEntity(crystal)

        this.dragon.broadcastMessage(
            Component.literal("An End Crystal has respawned!").withColor(0xFF55FF)
        )

        sendCrystalCount()
    }

    fun doesCrystalExist(vec: Vec3): Boolean {
        val aabb = AABB(vec.add(-0.5, -0.5, -0.5), vec.add(0.5, 0.5, 0.5)).inflate(1.5)
        return this.level.getEntitiesOfClass(EndCrystal::class.java, aabb).isNotEmpty()
    }

    fun onCrystalDestroyed(pos: BlockPos) {
        val key = this.crystalLocations.keys.minByOrNull { it.distanceToSqr(Vec3.atCenterOf(pos)) } ?: return
        this.crystalLocations[key] = 600
    }

    fun resetCooldowns() {
        this.crystalLocations.replaceAll { _, _ -> 0 }
    }

    fun getTotalCrystals(): Int {
        val pillarCrystals = this.crystalLocations.keys.count(::doesCrystalExist)
        val protectorCrystals = getProtectorCrystals()
        return pillarCrystals + protectorCrystals
    }

    private fun getProtectorCrystals(): Int {
        val center = Vec3.atCenterOf(this.dragon.fightOrigin)
        val aabb = AABB.ofSize(center, 400.0, 400.0, 400.0)

        return this.level
            .getEntitiesOfClass(CrystalProtector::class.java, aabb)
            .count(CrystalProtector::hasCrystal)
    }

    fun sendCrystalCount() {
        val count = getTotalCrystals()
        val message = Component.literal("✦ Crystals Remaining: $count").withStyle(ChatFormatting.DARK_PURPLE)

        this.dragon.broadcastMessage(message, true)
    }
}
