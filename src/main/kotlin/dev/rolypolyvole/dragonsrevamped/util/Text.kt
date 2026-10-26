package dev.rolypolyvole.dragonsrevamped.util

import net.kyori.adventure.platform.modcommon.MinecraftAudiences
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.minecraft.core.RegistryAccess
import net.minecraft.network.chat.Component

object Text {
    private val miniMessage = MiniMessage.miniMessage()
    private val serializer = MinecraftAudiences.nonWrappingSerializer { RegistryAccess.EMPTY }

    @JvmStatic
    fun parse(text: String, vararg tags: TagResolver): Component = serializer.serialize(miniMessage.deserialize(text, *tags))

    @JvmStatic
    fun placeholder(name: String, component: Component): TagResolver = Placeholder.component(name, serializer.deserialize(component))

    @JvmStatic
    fun placeholder(name: String, value: Any): TagResolver = Placeholder.unparsed(name, value.toString())
}
