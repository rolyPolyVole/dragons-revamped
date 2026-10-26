package dev.rolypolyvole.dragonsrevamped.dragon.attacks

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.dragon.flyTowards
import dev.rolypolyvole.dragonsrevamped.dragon.turnTowards
import dev.rolypolyvole.dragonsrevamped.util.broadcastSound
import dev.rolypolyvole.dragonsrevamped.util.highestBlockY
import dev.rolypolyvole.dragonsrevamped.util.nearbyPlayers
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.PowerParticleOption
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.AreaEffectCloud
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.enderdragon.phases.*
import net.minecraft.world.phys.Vec3
import kotlin.reflect.KClass

class LowDiveAttack(dragon: EnderDragon) : AbstractDragonAttack(dragon) {

    object LowDiveAttackType : DragonAttackType {
        override fun invalidPhases(): List<KClass<out DragonPhaseInstance>> = listOf(
            DragonChargePlayerPhase::class,
            DragonLandingApproachPhase::class,
            DragonLandingPhase::class,
            DragonDeathPhase::class,
            AbstractDragonSittingPhase::class
        )

        override fun canStart(dragon: EnderDragon, lastAttack: AbstractDragonAttack?): Boolean {
            return super.canStart(dragon, lastAttack) && lastAttack !is LowDiveAttack
        }

        override fun create(dragon: EnderDragon): AbstractDragonAttack {
            return LowDiveAttack(dragon)
        }
    }

    private enum class Stage {
        DIVE, HOVER, RISE
    }

    private val breath = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F)

    private var stage = Stage.DIVE
    private var stageTicks = 0
    private var target: ServerPlayer? = null
    private var destination = Vec3.ZERO
    private var shouldEnd = false

    override fun start(): Boolean {
        val player = dragon.nearbyPlayers().filter(::isValidTarget).randomOrNull() ?: return false

        target = player
        destination = hoverPointAbove(player)

        chargeTowards(destination)
        dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.7F + random * 0.2F)

        return true
    }

    override fun tick() {
        stageTicks++

        when (stage) {
            Stage.DIVE -> tickDive()
            Stage.HOVER -> tickHover()
            Stage.RISE -> if (dragon.position().distanceTo(destination) < 5.0 || stageTicks > MAX_RISE_TICKS) shouldEnd = true
        }
    }

    override fun beforeMove() {
        when (stage) {
            Stage.DIVE -> dragon.flyTowards(destination, DIVE_SPEED * ModConfig.current.diveSpeedMultiplier)
            Stage.HOVER -> dragon.deltaMovement = Vec3.ZERO
            Stage.RISE -> dragon.flyTowards(destination, RISE_SPEED)
        }
    }

    override fun end() {
        restoreNormalPhase()
    }

    override fun shouldEnd(): Boolean = shouldEnd

    private fun tickDive() {
        val player = target

        if (player == null || !isValidTarget(player)) {
            shouldEnd = true
            return
        }

        if (stageTicks % RETARGET_INTERVAL == 0) {
            destination = hoverPointAbove(player)
            chargeTowards(destination)
        }

        if (dragon.position().distanceTo(destination) < ARRIVAL_DISTANCE || stageTicks > MAX_DIVE_TICKS) {
            stage = Stage.HOVER
            stageTicks = 0
            hover()
        }
    }

    private fun tickHover() {
        val player = target?.takeIf(::isValidTarget)
        player?.let { dragon.turnTowards(it.position(), 6.0F) }

        pourBreath()

        if (stageTicks % CLOUD_INTERVAL == CLOUD_DELAY) {
            spawnBreathCloud()
        }

        if (stageTicks >= HOVER_TICKS) {
            val away = player?.let { dragon.position().subtract(it.position()).multiply(1.0, 0.0, 1.0).normalize() } ?: Vec3.ZERO
            destination = dragon.position().add(Vec3(away.x, 1.0, away.z).normalize().scale(RISE_DISTANCE))

            stage = Stage.RISE
            stageTicks = 0
            chargeTowards(destination)
        }
    }

    private fun pourBreath() {
        val mouth = mouthPosition()

        if (stageTicks % 20 == 1) {
            level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 4.0F, 0.6F + random * 0.2F)
        }

        repeat(PARTICLES_PER_TICK) {
            level.sendParticles(
                breath, true, true,
                mouth.x + (random - 0.5) * POUR_WIDTH,
                mouth.y + (random - 0.5) * POUR_WIDTH,
                mouth.z + (random - 0.5) * POUR_WIDTH,
                0,
                (random - 0.5) * SPREAD_SPEED,
                -(FALL_SPEED + random * FALL_SPEED_VARIATION),
                (random - 0.5) * SPREAD_SPEED,
                1.0
            )
        }
    }

    private fun spawnBreathCloud() {
        val mouth = mouthPosition()
        val groundY = BlockPos.containing(mouth).highestBlockY(level).y
        if (groundY <= level.minY) return

        AreaEffectCloud(level, mouth.x, groundY.toDouble(), mouth.z).apply {
            owner = dragon
            setCustomParticle(breath)
            radius = 6.0F
            duration = 120
            radiusPerTick = (8.0F - radius) / duration
            setPotionDurationScale(0.25F)
            addEffect(MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 1))
            level.addFreshEntity(this)
        }
    }

    private fun mouthPosition(): Vec3 = Vec3(dragon.head.x, dragon.head.getY(0.3), dragon.head.z)

    private fun hoverPointAbove(player: ServerPlayer): Vec3 =
        Vec3(player.x, player.blockPosition().highestBlockY(level).y + HOVER_HEIGHT, player.z)

    private fun isValidTarget(player: ServerPlayer): Boolean =
        !player.isCreative && !player.isSpectator && player.isAlive && !player.hasDisconnected() &&
            player.blockPosition().highestBlockY(level).y > level.minY

    private companion object {
        const val DIVE_SPEED = 0.9
        const val RISE_SPEED = 1.0
        const val HOVER_HEIGHT = 8.0
        const val ARRIVAL_DISTANCE = 4.0
        const val RETARGET_INTERVAL = 5
        const val MAX_DIVE_TICKS = 200
        const val HOVER_TICKS = 100
        const val MAX_RISE_TICKS = 100
        const val RISE_DISTANCE = 40.0

        const val PARTICLES_PER_TICK = 14
        const val POUR_WIDTH = 1.4
        const val SPREAD_SPEED = 0.25
        const val FALL_SPEED = 0.55
        const val FALL_SPEED_VARIATION = 0.35

        const val CLOUD_INTERVAL = 40
        const val CLOUD_DELAY = 12
    }
}
