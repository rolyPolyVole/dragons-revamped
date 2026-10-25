package dev.rolypolyvole.dragonsrevamped

import dev.rolypolyvole.dragonsrevamped.mixin.EnderDragonFightAccessor
import dev.rolypolyvole.dragonsrevamped.worldgen.CustomEndSpikes
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.status.ChunkStatus


class DragonsRevamped : ModInitializer {

    companion object {
        const val MOD_ID = "dragons-revamped"
    }

    override fun onInitialize() {
        println("Dragons Revamped has been initialized!")

        ServerLifecycleEvents.SERVER_STARTED.register(this::createCustomEndSpikes)
    }

    private fun createCustomEndSpikes(server: MinecraftServer) {
        val end = server.getLevel(Level.END)!!

        val firstGeneration = (end.dragonFight as EnderDragonFightAccessor).`dragonsrevamped$needsStateScanning`()
        if (!firstGeneration) return

        for (x in -8..8) {
            for (z in -8..8) {
                end.getChunk(x, z, ChunkStatus.FULL, true)
            }
        }

        CustomEndSpikes.regenerateSpikes(end)
    }
}
