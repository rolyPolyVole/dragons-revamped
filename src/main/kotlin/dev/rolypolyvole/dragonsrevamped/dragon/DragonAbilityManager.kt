package dev.rolypolyvole.dragonsrevamped.dragon

import dev.rolypolyvole.dragonsrevamped.dragon.abilities.CrystalRespawnAbility
import dev.rolypolyvole.dragonsrevamped.dragon.abilities.DragonRageAbility
import dev.rolypolyvole.dragonsrevamped.dragon.abilities.HunterSpawnAbility
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.boss.enderdragon.EnderDragon

class DragonAbilityManager(dragon: EnderDragon) {
    private val level = dragon.level() as ServerLevel

    val rageAbility = DragonRageAbility(dragon, this)
    val crystalRespawnAbility = CrystalRespawnAbility(dragon, this)
    val hunterSpawnAbility = HunterSpawnAbility(dragon, this)

    fun tick() {
        rageAbility.tick()
        if (rageAbility.isActive()) return

        crystalRespawnAbility.tick()
        hunterSpawnAbility.tick()
    }

    fun sendParticles(
        particle: ParticleOptions,
        x: Double, y: Double, z: Double,
        count: Int, dx: Double = 0.0, dy: Double = 0.0, dz: Double = 0.0, speed: Double = 0.0
    ) {
        level.sendParticles(particle, true, true, x, y, z, count, dx, dy, dz, speed)
    }
}
