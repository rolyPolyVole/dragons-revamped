package dev.rolypolyvole.dragonsrevamped

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.worldgen.CustomEndSpikes
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.world.level.Level
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DragonsRevamped : ModInitializer {

    companion object {
        const val MOD_ID = "dragons-revamped"

        @JvmField
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)
    }

    override fun onInitialize() {
        ModConfig.load()

        ServerLifecycleEvents.SERVER_STARTING.register { ModConfig.load() }

        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            server.getLevel(Level.END)?.let(CustomEndSpikes::placeInitialSpikes)
        }

        LOGGER.info("Dragons Revamped has been initialized!")
    }
}
