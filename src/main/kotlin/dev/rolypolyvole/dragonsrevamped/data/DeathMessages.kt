package dev.rolypolyvole.dragonsrevamped.data

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.util.Text
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource

object DeathMessages {

    @JvmStatic
    fun forDeath(player: ServerPlayer, source: DamageSource): Component? {
        val attacker = source.directEntity ?: return null
        val messages = ModConfig.current.messages

        val template = when {
            source.`is`(DragonDamageTypes.DRAGON_LIGHTNING) -> messages.lightningKill
            source.`is`(DragonDamageTypes.DRAGON_BODY_HIT) -> messages.bodyKill
            else -> return null
        }

        return Text.parse(
            template,
            Text.placeholder("player", player.displayName),
            Text.placeholder("dragon", attacker.displayName)
        )
    }
}
