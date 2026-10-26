package dev.rolypolyvole.dragonsrevamped.dragon

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.dragon.attacks.*
import dev.rolypolyvole.dragonsrevamped.util.broadcastSound
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
        List(4) { LowDiveAttack.LowDiveAttackType },
        List(fighters.coerceAtMost(4)) { EnragedChargeAttack.EnragedChargeAttackType }
    ).flatten()

    private var currentAttack: AbstractDragonAttack? = null
    private var lastAttack: AbstractDragonAttack? = null

    private var ticksUntilNextAttack = 300
    private var ticksUntilRoar = nextRoarDelay()
    private var returningToCenter = false

    val speedMultiplier: Float
        get() = currentAttack?.getSpeedMultiplier() ?: 1.0F
    val turnSpeedMultiplier: Float
        get() = currentAttack?.getTurnSpeedMultiplier() ?: 1.0F

    fun tick() {
        tickRoar()

        if (dragon.isOffside() && !returningToCenter) {
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
            dragon.flyTowards(dragon.arenaCenter(RETURN_HEIGHT), 1.2)
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

    private fun startReturningToCenter() {
        currentAttack?.let(::finishAttack)
        returningToCenter = true

        dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)
    }

    private fun tickReturn() {
        val center = dragon.arenaCenter(RETURN_HEIGHT)
        dragon.turnTowards(center, 10.0F)

        if (dragon.position().distanceTo(center) < RETURNED_DISTANCE) {
            returningToCenter = false
            dragon.phaseManager.setPhase(EnderDragonPhase.HOLDING_PATTERN)
        }
    }

    private fun tickRoar() {
        if (--ticksUntilRoar > 0) return

        ticksUntilRoar = nextRoarDelay()
        dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.8F + Math.random().toFloat() * 0.3F)
    }

    private fun nextRoarDelay(): Int = (200..400).random()

    private companion object {
        const val RETURN_HEIGHT = 80.0
        const val RETURNED_DISTANCE = 40.0
    }
}
