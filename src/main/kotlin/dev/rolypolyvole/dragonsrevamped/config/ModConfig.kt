package dev.rolypolyvole.dragonsrevamped.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import dev.rolypolyvole.dragonsrevamped.DragonsRevamped
import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ModConfig {
    private val gson = GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create()
    private val path = FabricLoader.getInstance().configDir.resolve("${DragonsRevamped.MOD_ID}.json")

    @JvmStatic
    var current = DragonConfig()
        private set

    fun load() {
        current = try {
            if (path.exists()) gson.fromJson(path.readText(), DragonConfig::class.java) ?: DragonConfig() else DragonConfig()
        } catch (exception: JsonParseException) {
            DragonsRevamped.LOGGER.error("Couldn't parse {}, using the default config until it's fixed", path, exception)
            return
        }

        try {
            path.writeText(gson.toJson(current))
        } catch (exception: IOException) {
            DragonsRevamped.LOGGER.error("Couldn't write {}", path, exception)
        }
    }
}
