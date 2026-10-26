package dev.rolypolyvole.dragonsrevamped.dragon.attacks

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.data.DragonDamageTypes
import dev.rolypolyvole.dragonsrevamped.dragon.flyTowards
import dev.rolypolyvole.dragonsrevamped.util.broadcastSound
import dev.rolypolyvole.dragonsrevamped.util.highestBlockY
import dev.rolypolyvole.dragonsrevamped.util.nearbyPlayers
import dev.rolypolyvole.dragonsrevamped.util.sendSound
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.enderdragon.phases.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import kotlin.reflect.KClass

class LightningAttack(dragon: EnderDragon) : AbstractDragonAttack(dragon) {

    private companion object {
        const val STRIKE_INTERVAL = 30
    }

    object LightningAttackType : DragonAttackType {
        override fun invalidPhases(): List<KClass<out DragonPhaseInstance>> = listOf(
            DragonLandingApproachPhase::class,
            DragonLandingPhase::class,
            DragonDeathPhase::class,
            AbstractDragonSittingPhase::class
        )

        override fun canStart(dragon: EnderDragon, lastAttack: AbstractDragonAttack?): Boolean {
            return super.canStart(dragon, lastAttack) && lastAttack !is LightningAttack
        }

        override fun create(dragon: EnderDragon): AbstractDragonAttack {
            return LightningAttack(dragon)
        }
    }

    private lateinit var outpost: Vec3
    private var reachedOutpost = false

    private var ticks = 0
    private var elapsedTicks = 0
    private var shouldEnd: Boolean = false

    override fun tick() {
        if (elapsedTicks++ % STRIKE_INTERVAL == 0) {
            strikeDragon()
        }

        if (!reachedOutpost) return

        if (ticks++ == 0) {
            hover()
            dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, pitch = 0.8F + random * 0.3F)
        }

        dragon.nearbyPlayers().filter(::isPlayerExposed).forEach {
            it.sendSound(SoundEvents.FLINTANDSTEEL_USE, pitch = 2.0F)
        }

        if (ticks < 50) return

        strikeDragon()
        dragon.broadcastSound(SoundEvents.LIGHTNING_BOLT_THUNDER, pitch = 0.8F + random * 0.3F)

        dragon.nearbyPlayers()
            .filter(::isPlayerExposed)
            .forEach(::strikePlayer)

        shouldEnd = true
    }

    override fun beforeMove() {
        if (reachedOutpost) {
            dragon.deltaMovement = Vec3.ZERO
            return
        }

        if (outpost.distanceToSqr(dragon.position()) < 256.0) {
            reachedOutpost = true
            dragon.deltaMovement = Vec3.ZERO
            return
        }

        dragon.flyTowards(outpost, 1.0)
    }

    override fun start(): Boolean {
        outpost = getOutpostLocation()

        chargeTowards(outpost)
        dragon.broadcastSound(SoundEvents.LIGHTNING_BOLT_THUNDER, pitch = 0.8F + random * 0.3F)

        return true
    }

    override fun end() {
        restoreNormalPhase()
    }

    override fun shouldEnd(): Boolean = shouldEnd

    private fun getOutpostLocation(): Vec3 {
        return Vec3.atCenterOf(dragon.fightOrigin).add(
            random * 100.0 - 50.0,
            115.0 + random * 35.0,
            random * 100.0 - 50.0
        )
    }

    private fun isPlayerExposed(player: Player): Boolean {
        if (player.isCreative || player.isSpectator || !player.isAlive) return false

        val blockY = player.blockPosition().highestBlockY(level).y
        val playerY = player.blockPosition().y

        return playerY >= blockY
    }

    private fun strikePlayer(player: Player) {
        EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.MOB_SUMMONED)?.let {
            it.snapTo(player.position())
            it.setVisualOnly(true)
            level.addFreshEntity(it)
        }

        val source = DragonDamageTypes.dragonLightning(level, dragon)
        val damage = ModConfig.current.damage.lightning.forDifficulty(level.difficulty)
        player.hurtServer(level, source, damage)
    }

    private fun strikeDragon() {
        EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.MOB_SUMMONED)?.let {
            it.snapTo(dragon.position())
            it.setVisualOnly(true)
            level.addFreshEntity(it)
        }
    }
}
