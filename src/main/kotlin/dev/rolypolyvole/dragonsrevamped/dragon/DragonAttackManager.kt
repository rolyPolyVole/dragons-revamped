package dev.rolypolyvole.dragonsrevamped.dragon

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.dragon.attacks.*
import dev.rolypolyvole.dragonsrevamped.util.arenaCenter
import dev.rolypolyvole.dragonsrevamped.util.broadcastSound
import dev.rolypolyvole.dragonsrevamped.util.flyTowards
import dev.rolypolyvole.dragonsrevamped.util.isBelowArena
import dev.rolypolyvole.dragonsrevamped.util.isOutsideArena
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
import kotlin.math.sqrt

class DragonAttackManager(private val dragon: EnderDragon) {
    private val fighters = ModConfig.current.fighters

    private val attacks = listOf(
        List(12) { ChargeAttack.ChargeAttackType },
        List(5) { FireballAttack.FireballAttackType },
        List(3) { LightningAttack.LightningAttackType },
        List(4) { BreathAttack.LowDiveAttackType },
        List(fighters.coerceAtMost(4)) { EnragedChargeAttack.EnragedChargeAttackType }
    ).flatten()

    private var currentAttack: AbstractDragonAttack? = null
    private var lastAttack: AbstractDragonAttack? = null

    private var ticksUntilNextAttack = 300
    private var ticksUntilRoar = nextRoarDelay()
    private var returningToCenter = false

    val speedMultiplier: Float; get() = currentAttack?.getSpeedMultiplier() ?: 1.0F
    val turnSpeedMultiplier: Float; get() = currentAttack?.getTurnSpeedMultiplier() ?: 1.0F

    fun tick() {
        tickRoar()

        if (!returningToCenter && shouldReturnToCenter()) {
            startReturningToCenter()
        }

        if (returningToCenter) {
            tickReturn()
            return
        }

        currentAttack?.let {
            it.tick()

            if (it.shouldEnd()) {
                finishAttack(it)
            }

            return
        }

        if (ticksUntilNextAttack > 0) {
            ticksUntilNextAttack--
            return
        }

        val picked = attacks
            .filter { it.canStart(dragon, lastAttack) }
            .randomOrNull()?.create(dragon)

        if (picked != null && picked.start()) {
            currentAttack = picked
        } else {
            ticksUntilNextAttack = 20
        }
    }

    fun onBeforeMove() {
        if (returningToCenter) {
            dragon.flyTowards(dragon.arenaCenter(RETURN_HEIGHT), RETURN_SPEED * ModConfig.current.dragon.diveSpeedMultiplier)
            return
        }

        currentAttack?.beforeMove()
    }

    private fun finishAttack(attack: AbstractDragonAttack) {
        attack.end()

        lastAttack = attack
        currentAttack = null
        ticksUntilNextAttack = ((80..200).random() / sqrt(fighters.toDouble())).toInt()
    }

    private fun shouldReturnToCenter(): Boolean =
        dragon.isBelowArena() || (dragon.isOutsideArena() && currentAttack?.allowsReturnToCenter() != false)

    private fun startReturningToCenter() {
        currentAttack?.let(::finishAttack)
        returningToCenter = true

        chargeAtCenter()
        dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.8F + Math.random().toFloat() * 0.3F)
    }

    private fun chargeAtCenter() {
        val center = dragon.arenaCenter(RETURN_HEIGHT)

        dragon.phaseManager.setPhase(EnderDragonPhase.CHARGING_PLAYER)
        dragon.phaseManager.getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(center)
    }

    private fun tickReturn() {
        val center = dragon.arenaCenter(RETURN_HEIGHT)

        if (dragon.phaseManager.currentPhase.phase != EnderDragonPhase.CHARGING_PLAYER) chargeAtCenter()
        dragon.lookAt(EntityAnchorArgument.Anchor.FEET, dragon.position().add(dragon.position().subtract(center)))

        if (dragon.position().distanceTo(center) < RETURNED_DISTANCE) {
            returningToCenter = false
            dragon.phaseManager.setPhase(EnderDragonPhase.HOLDING_PATTERN)
        }
    }

    private fun tickRoar() {
        if (currentAttack?.allowsRoaring() == false) return
        if (--ticksUntilRoar > 0) return

        ticksUntilRoar = nextRoarDelay()
        dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.8F + Math.random().toFloat() * 0.3F)
    }

    private fun nextRoarDelay(): Int = (200..400).random()

    private companion object {
        const val RETURN_HEIGHT = 18.0
        const val RETURN_SPEED = 1.8
        const val RETURNED_DISTANCE = 40.0
    }
}
