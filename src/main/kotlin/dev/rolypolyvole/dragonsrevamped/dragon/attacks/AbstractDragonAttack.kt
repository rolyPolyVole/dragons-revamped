package dev.rolypolyvole.dragonsrevamped.dragon.attacks

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
import net.minecraft.world.phys.Vec3
import kotlin.reflect.KClass

abstract class AbstractDragonAttack(protected val dragon: EnderDragon) {
    private val phaseManager = dragon.phaseManager

    protected val random: Float; get() = Math.random().toFloat()

    protected val level: ServerLevel
        get() = dragon.level() as ServerLevel

    protected fun chargeTowards(target: Vec3) {
        phaseManager.setPhase(EnderDragonPhase.CHARGING_PLAYER)
        phaseManager.getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(target)
    }

    protected fun hover() = phaseManager.setPhase(EnderDragonPhase.HOVERING)
    protected fun restoreNormalPhase() = phaseManager.setPhase(EnderDragonPhase.HOLDING_PATTERN)

    abstract fun tick()
    open fun beforeMove() {}

    open fun getSpeedMultiplier(): Float = 1.0f
    open fun getTurnSpeedMultiplier(): Float = 1.0f

    abstract fun shouldEnd(): Boolean

    abstract fun start(): Boolean
    abstract fun end()
}

interface DragonAttackType {
    fun canStart(dragon: EnderDragon, lastAttack: AbstractDragonAttack?): Boolean {
        return invalidPhases().none { it.isInstance(dragon.phaseManager.currentPhase) }
    }

    fun invalidPhases(): List<KClass<out DragonPhaseInstance>>
    fun create(dragon: EnderDragon): AbstractDragonAttack
}
