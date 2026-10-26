package dev.rolypolyvole.dragonsrevamped.data

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity

object DeathMessages {
    private val miniMessage = MiniMessage.miniMessage()

    @JvmStatic
    fun forDeath(player: ServerPlayer, source: DamageSource): Component? {
        val attacker = source.directEntity ?: return null
        val messages = ModConfig.current.deathMessages

        val template = when {
            source.`is`(DragonDamageTypes.DRAGON_LIGHTNING) -> messages.lightning
            source.`is`(DragonDamageTypes.DRAGON_BODY_HIT) -> messages.bodyHit
            else -> return null
        }

        return format(template, player, attacker)
    }

    private fun format(template: String, player: ServerPlayer, attacker: Entity): Component {
        val audiences = MinecraftServerAudiences.of(player.level().server)

        val message = miniMessage.deserialize(
            template,
            Placeholder.component("player", audiences.asAdventure(player.displayName)),
            Placeholder.component("dragon", audiences.asAdventure(attacker.displayName))
        )

        return audiences.asNative(message)
    }
}
