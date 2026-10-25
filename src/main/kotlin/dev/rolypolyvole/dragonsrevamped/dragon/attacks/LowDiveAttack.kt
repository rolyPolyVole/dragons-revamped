package dev.rolypolyvole.dragonsrevamped.dragon.attacks

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.dragon.flyTowards
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
            return super.canStart(dragon, lastAttack) && lastAttack !is LowDiveAttack && groundedTargets(dragon).isNotEmpty()
        }

        override fun create(dragon: EnderDragon): AbstractDragonAttack {
            return LowDiveAttack(dragon)
        }

        fun groundedTargets(dragon: EnderDragon): List<ServerPlayer> = dragon.nearbyPlayers().filter {
            !it.isCreative && !it.isSpectator && it.isAlive && it.onGround()
        }
    }

    private enum class Stage {
        DIVE, LINGER
    }

    private val breath = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F)

    private var stage = Stage.DIVE
    private var stageTicks = 0
    private var destination = Vec3.ZERO
    private var shouldEnd = false

    override fun start(): Boolean {
        val player = LowDiveAttackType.groundedTargets(dragon).randomOrNull() ?: return false
        destination = hoverPointAbove(player)

        chargeTowards(destination)
        dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.7F + random * 0.2F)

        return true
    }

    override fun tick() {
        stageTicks++

        when (stage) {
            Stage.DIVE -> tickDive()
            Stage.LINGER -> tickLinger()
        }
    }

    override fun beforeMove() {
        when (stage) {
            Stage.DIVE -> dragon.flyTowards(destination, DIVE_SPEED * ModConfig.current.diveSpeedMultiplier)
            Stage.LINGER -> limitClimbRate()
        }
    }

    override fun end() {
        if (stage == Stage.DIVE) restoreNormalPhase()
    }

    override fun shouldEnd(): Boolean = shouldEnd

    private fun tickDive() {
        if (dragon.position().distanceTo(destination) < ARRIVAL_DISTANCE || stageTicks > MAX_DIVE_TICKS) {
            stage = Stage.LINGER
            stageTicks = 0
            restoreNormalPhase()
        }
    }

    private fun tickLinger() {
        val phase = dragon.phaseManager.currentPhase

        if (stageTicks >= LINGER_TICKS || phase.isSitting || phase is DragonLandingApproachPhase || phase is DragonLandingPhase) {
            shouldEnd = true
            return
        }

        pourBreath()

        if (stageTicks % CLOUD_INTERVAL == CLOUD_DELAY) {
            spawnBreathCloud()
        }
    }

    private fun limitClimbRate() {
        val movement = dragon.deltaMovement
        dragon.deltaMovement = Vec3(movement.x, movement.y.coerceIn(-MAX_VERTICAL_SPEED, MAX_VERTICAL_SPEED), movement.z)
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

    private companion object {
        const val DIVE_SPEED = 0.9
        const val HOVER_HEIGHT = 8.0
        const val ARRIVAL_DISTANCE = 4.0
        const val MAX_DIVE_TICKS = 200
        const val LINGER_TICKS = 100
        const val MAX_VERTICAL_SPEED = 0.08

        const val PARTICLES_PER_TICK = 14
        const val POUR_WIDTH = 1.4
        const val SPREAD_SPEED = 0.25
        const val FALL_SPEED = 0.55
        const val FALL_SPEED_VARIATION = 0.35

        const val CLOUD_INTERVAL = 40
        const val CLOUD_DELAY = 12
    }
}
