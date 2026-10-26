package dev.rolypolyvole.dragonsrevamped.dragon.attacks

import dev.rolypolyvole.dragonsrevamped.dragon.flyTowards
import dev.rolypolyvole.dragonsrevamped.dragon.turnTowards
import dev.rolypolyvole.dragonsrevamped.util.broadcastSound
import dev.rolypolyvole.dragonsrevamped.util.highestBlockY
import dev.rolypolyvole.dragonsrevamped.util.nearbyPlayers
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.reflect.KClass

class LowDiveAttack(dragon: EnderDragon) : AbstractDragonAttack(dragon) {

    object LowDiveAttackType : DragonAttackType {
        override fun invalidPhases(): List<KClass<out DragonPhaseInstance>> = listOf(
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
        APPROACH, DESCEND, STRAFE
    }

    private var stage = Stage.APPROACH
    private var target: ServerPlayer? = null
    private var waypoint = Vec3.ZERO

    private var ticks = 0
    private var strafeTicks = 0
    private var breathCooldown = 0
    private var orbitAngle = 0.0
    private var shouldEnd = false

    override fun start(): Boolean {
        val player = findTargets().randomOrNull() ?: return false

        target = player
        orbitAngle = atan2(dragon.z - player.z, dragon.x - player.x)

        hover()
        dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.7F + random * 0.2F)

        return true
    }

    override fun tick() {
        ticks++

        if (ticks > MAX_DURATION) {
            shouldEnd = true
            return
        }

        if (stage == Stage.STRAFE && strafeTicks++ % RETARGET_INTERVAL == 0) {
            target = findTargets().minByOrNull { it.distanceToSqr(dragon) }
        }

        val player = target

        if (player == null || !isValidTarget(player)) {
            shouldEnd = true
            return
        }

        val cruiseHeight = (surfaceY(player) + CRUISE_HEIGHT).toDouble()
        val horizontalDistance = horizontalDistance(dragon.position(), player.position())

        when (stage) {
            Stage.APPROACH -> {
                val awayFromPlayer = Vec3(cos(orbitAngle), 0.0, sin(orbitAngle)).scale(APPROACH_DISTANCE)
                waypoint = player.position().add(awayFromPlayer).with(Direction.Axis.Y, dragon.y)

                if (horizontalDistance(dragon.position(), waypoint) < 8.0) stage = Stage.DESCEND
            }

            Stage.DESCEND -> {
                waypoint = Vec3(dragon.x, cruiseHeight, dragon.z)

                if (dragon.y - cruiseHeight < 2.0) stage = Stage.STRAFE
            }

            Stage.STRAFE -> {
                orbitAngle += ORBIT_SPEED
                val offset = Vec3(cos(orbitAngle), 0.0, sin(orbitAngle)).scale(ORBIT_RADIUS)
                waypoint = player.position().add(offset).with(Direction.Axis.Y, cruiseHeight)

                if (breathCooldown > 0) {
                    breathCooldown--
                } else if (horizontalDistance < BREATH_RANGE) {
                    spitBreath(player)
                    breathCooldown = BREATH_INTERVAL
                }
            }
        }

        dragon.turnTowards(if (stage == Stage.STRAFE) player.position() else waypoint, 12.0F)
    }

    override fun beforeMove() {
        val speed = when (stage) {
            Stage.APPROACH -> 1.0
            Stage.DESCEND -> 0.7
            Stage.STRAFE -> 0.6
        }

        dragon.flyTowards(waypoint, speed)
    }

    override fun end() {
        restoreNormalPhase()
    }

    override fun shouldEnd(): Boolean = shouldEnd

    private fun spitBreath(player: ServerPlayer) {
        val mouth = Vec3(dragon.head.x, dragon.head.getY(0.5), dragon.head.z)
        val towardsPlayer = player.position().subtract(mouth).multiply(1.0, 0.0, 1.0).normalize()
        val ahead = mouth.add(towardsPlayer.scale(BREATH_REACH))
        val groundY = BlockPos.containing(ahead).highestBlockY(level).y.takeIf { it > level.minY } ?: surfaceY(player)
        val landing = Vec3(ahead.x, groundY.toDouble(), ahead.z)

        val particle = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F)

        for (step in 0..BREATH_PARTICLE_STEPS) {
            val point = mouth.lerp(landing, step / BREATH_PARTICLE_STEPS.toDouble())
            level.sendParticles(particle, true, true, point.x, point.y, point.z, 4, 0.3, 0.3, 0.3, 0.02)
        }

        AreaEffectCloud(level, landing.x, landing.y, landing.z).apply {
            owner = dragon
            setCustomParticle(particle)
            radius = 7.0F
            duration = 200
            radiusPerTick = (9.0F - radius) / duration
            setPotionDurationScale(0.25F)
            addEffect(MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 1))
            level.addFreshEntity(this)
        }

        level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 4.0F, 0.8F + random * 0.2F)
    }

    private fun findTargets(): List<ServerPlayer> = dragon.nearbyPlayers().filter(::isValidTarget)

    private fun isValidTarget(player: ServerPlayer): Boolean =
        !player.isCreative && !player.isSpectator && player.isAlive && !player.hasDisconnected() && surfaceY(player) > level.minY

    private fun surfaceY(player: ServerPlayer): Int = player.blockPosition().highestBlockY(level).y

    private fun horizontalDistance(from: Vec3, to: Vec3): Double = from.multiply(1.0, 0.0, 1.0).distanceTo(to.multiply(1.0, 0.0, 1.0))

    private companion object {
        const val MAX_DURATION = 20 * 20
        const val RETARGET_INTERVAL = 40
        const val CRUISE_HEIGHT = 10
        const val APPROACH_DISTANCE = 25.0
        const val ORBIT_RADIUS = 14.0
        const val ORBIT_SPEED = 0.03
        const val BREATH_RANGE = 22.0
        const val BREATH_INTERVAL = 30
        const val BREATH_REACH = 10.0
        const val BREATH_PARTICLE_STEPS = 20
    }
}
