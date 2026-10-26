package dev.rolypolyvole.dragonsrevamped.dragon.abilities

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.dragon.DragonAbilityManager
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector
import dev.rolypolyvole.dragonsrevamped.util.Text
import dev.rolypolyvole.dragonsrevamped.util.broadcastMessage
import dev.rolypolyvole.dragonsrevamped.util.highestBlockY
import dev.rolypolyvole.dragonsrevamped.util.nearbyPlayers
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

class HunterSpawnAbility(
    dragon: EnderDragon,
    manager: DragonAbilityManager
) : AbstractDragonAbility(dragon, manager) {

    private val level = dragon.level() as ServerLevel

    private var cooldown = calculateCooldown()

    private fun calculateCooldown(): Int {
        val config = ModConfig.current

        val base = (1200..1500).random()
        val scaled = 1200 / sqrt(config.fighters.toDouble())

        return ((base + scaled) / config.crystalProtectors.spawnRate).toInt()
    }

    override fun tick() {
        if (cooldown > 0) {
            cooldown--
        } else {
            if (aliveHunters() < ModConfig.current.crystalProtectorLimit) spawnHunter()
            cooldown = calculateCooldown()
        }
    }

    private fun aliveHunters(): Int {
        val area = AABB.ofSize(Vec3.atCenterOf(dragon.fightOrigin), 400.0, 400.0, 400.0)
        return level.getEntitiesOfClass(CrystalProtector::class.java, area).count { it.isHunter && it.isAlive }
    }

    private fun spawnHunter() {
        val column = dragon.fightOrigin.offset((-40..40).random(), 0, (-40..40).random())
        val spawnPos = Vec3.atBottomCenterOf(column.highestBlockY(level))

        CrystalProtector(level).let {
            it.setPos(spawnPos)
            it.spawnWithMount(level)
            it.target = highestPlayer()
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
            Text.parse("<dark_purple>A Crystal Protector joins the battle!")
        )

        manager.crystalRespawnAbility.sendCrystalCount()
    }

    private fun highestPlayer(): ServerPlayer? = dragon.nearbyPlayers()
        .filter { !it.isCreative && !it.isSpectator && it.isAlive }
        .maxByOrNull { it.y }
}
