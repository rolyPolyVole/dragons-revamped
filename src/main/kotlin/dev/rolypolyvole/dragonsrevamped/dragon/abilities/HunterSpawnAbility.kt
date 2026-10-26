package dev.rolypolyvole.dragonsrevamped.dragon.abilities

import dev.rolypolyvole.dragonsrevamped.dragon.DragonAbilityManager
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector
import dev.rolypolyvole.dragonsrevamped.util.broadcastMessage
import dev.rolypolyvole.dragonsrevamped.util.highestBlockY
import dev.rolypolyvole.dragonsrevamped.util.nearbyPlayers
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

class HunterSpawnAbility(
    dragon: EnderDragon,
    manager: DragonAbilityManager
) : AbstractDragonAbility(dragon, manager) {

    private val level = dragon.level() as ServerLevel

    private var cooldown = calculateCooldown()

    private fun calculateCooldown(): Int {
        val playerCount = dragon.nearbyPlayers().size.coerceAtLeast(1)

        val base = (1200..1500).random()
        val scaled = (1200 / sqrt(playerCount.toDouble())).toInt()

        return base + scaled
    }

    override fun tick() {
        if (cooldown > 0) {
            cooldown--
        } else {
            spawnHunter()
            cooldown = calculateCooldown()
        }
    }

    private fun spawnHunter() {
        val column = dragon.fightOrigin.offset((-40..40).random(), 0, (-40..40).random())
        val spawnPos = Vec3.atBottomCenterOf(column.highestBlockY(level))

        CrystalProtector(level).let {
            it.setPos(spawnPos)
            it.spawnWithMount(level)
        }

        EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.MOB_SUMMONED)?.let {
            it.snapTo(spawnPos)
            it.setVisualOnly(true)
            level.addFreshEntity(it)
        }

        manager.sendParticles(
            ParticleTypes.EXPLOSION_EMITTER,
            spawnPos.x, spawnPos.y + 1.0, spawnPos.z,
            1
        )

        dragon.broadcastMessage(
            Component.literal("A Crystal Protector joins the battle!").withColor(0xAA00AA)
        )

        manager.crystalRespawnAbility.sendCrystalCount()
    }
}
