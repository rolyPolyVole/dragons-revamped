package dev.rolypolyvole.dragonsrevamped.worldgen

import dev.rolypolyvole.dragonsrevamped.DragonsRevamped
import dev.rolypolyvole.dragonsrevamped.mixin.accessor.EnderDragonFightAccessor
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.WorldGenRegion
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.chunk.status.ChunkStatus
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature
import net.minecraft.world.phys.Vec3

object CustomEndSpikes {
    private const val ARENA_CHUNK_RADIUS = 8

    fun getCrystalLocations(level: ServerLevel): List<Vec3> = getCrystalSites(level).map { it.position }

    fun getCrystalSites(level: ServerLevel): List<CrystalSite> = EndSpikeFeature.getSpikesForLevel(level).map {
        CrystalSite(Vec3(it.centerX + 0.5, it.height.toDouble(), it.centerZ + 0.5), it.isGuarded)
    }

    class CrystalSite(val position: Vec3, val guarded: Boolean)

    @JvmStatic
    fun place(level: ServerLevelAccessor, feature: EndSpikeFeature, spike: EndSpikeFeature.EndSpike) {
        if (level is WorldGenRegion) return

        SpikeBuilder(level, spike).build(feature.crystalInvulnerable(), feature.crystalBeamTarget().orElse(null))
    }

    fun placeInitialSpikes(end: ServerLevel) {
        val fight = end.dragonFight as? EnderDragonFightAccessor? ?: return
        if (!fight.needsStateScanning()) return

        for (x in -ARENA_CHUNK_RADIUS..ARENA_CHUNK_RADIUS) {
            for (z in -ARENA_CHUNK_RADIUS..ARENA_CHUNK_RADIUS) {
                end.getChunk(x, z, ChunkStatus.FULL, true)
            }
        }

        val missing = EndSpikeFeature.getSpikesForLevel(end)
            .filterNot { TipStructure.isBuilt(end, it.centerX, it.height, it.centerZ) }

        missing.forEach { SpikeBuilder(end, it).build(crystalInvulnerable = false, crystalBeamTarget = null) }

        if (missing.isNotEmpty()) {
            DragonsRevamped.LOGGER.info("Placed {} End spikes", missing.size)
        }
    }
}
