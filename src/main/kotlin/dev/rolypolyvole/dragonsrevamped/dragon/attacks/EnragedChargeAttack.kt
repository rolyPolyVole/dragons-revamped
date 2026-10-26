package dev.rolypolyvole.dragonsrevamped.dragon.attacks

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.util.broadcastSound
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.boss.enderdragon.EnderDragon

class EnragedChargeAttack(dragon: EnderDragon) : AbstractDragonAttack(dragon) {

    object EnragedChargeAttackType : DragonAttackType {
        override fun canStart(dragon: EnderDragon, lastAttack: AbstractDragonAttack?): Boolean {
            return super.canStart(dragon, lastAttack) && lastAttack !is EnragedChargeAttack
        }

        override fun create(dragon: EnderDragon): AbstractDragonAttack {
            return EnragedChargeAttack(dragon)
        }
    }

    private var roarTicks = 0

    private var chargesRemaining = 0
    private var currentCharge: ChargeAttack? = null
    private var shouldEnd = false

    override fun start(): Boolean {
        chargesRemaining = 4.coerceAtLeast(ModConfig.current.fighters)

        return startNextCharge()
    }

    private fun startNextCharge(): Boolean {
        if (chargesRemaining <= 0) {
            shouldEnd = true
            return false
        }

        val charge = ChargeAttack(dragon)

        if (!charge.start()) {
            shouldEnd = true
            return false
        }

        charge.chargeSpeedMultiplier = 1.3

        currentCharge = charge
        chargesRemaining--
        roarTicks = 0
        return true
    }

    override fun tick() {
        if (roarTicks++ <= 10) {
            dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, volume = 2.0F, pitch = 0.5F + roarTicks * 0.2F)
        }

        val charge = currentCharge ?: run { shouldEnd = true; return }

        charge.tick()

        if (charge.shouldEnd()) {
            charge.end()

            if (chargesRemaining > 0) {
                startNextCharge()
            } else {
                shouldEnd = true
            }
        }
    }

    override fun beforeMove() {
        currentCharge?.beforeMove()
    }

    override fun end() {
        currentCharge?.end()
        restoreNormalPhase()
    }

    override fun getSpeedMultiplier(): Float = currentCharge?.getSpeedMultiplier() ?: 1.0f
    override fun getTurnSpeedMultiplier(): Float = currentCharge?.getTurnSpeedMultiplier() ?: 1.0f

    override fun shouldEnd(): Boolean = shouldEnd
}
