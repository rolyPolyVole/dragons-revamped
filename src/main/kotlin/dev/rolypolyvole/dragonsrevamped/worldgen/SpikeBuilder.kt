package dev.rolypolyvole.dragonsrevamped.worldgen

import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

class SpikeBuilder(private val level: ServerLevelAccessor, private val spike: EndSpikeFeature.EndSpike) {
    private val random = randomFor(level.level.seed, spike)
    private val shape = SpikeShape(spike, level.minY, random)
    private val pos = BlockPos.MutableBlockPos()

    fun build(crystalInvulnerable: Boolean, crystalBeamTarget: BlockPos?) {
        placeIsland()
        placeSpire()
        TipStructure.place(level, shape.tipX, shape.tipBaseY, shape.tipZ)

        if (spike.isGuarded) {
            placeCage()
        }

        spawnCrystal(crystalInvulnerable, crystalBeamTarget)
    }

    private fun placeIsland() {
        val topRadius = shape.baseRadius + 2
        val centerX = shape.centerXAt(shape.bottomY)
        val centerZ = shape.centerZAt(shape.bottomY)

        for (depth in 0..topRadius) {
            val progress = depth / topRadius.toFloat()
            val radius = topRadius * sqrt(1.0f - progress * progress)
            if (radius < 0.5f) break

            val y = shape.bottomY - depth
            forEachInDisc(centerX, centerZ, radius) { x, z -> setBlock(x, y, z, Blocks.END_STONE.defaultBlockState()) }
        }
    }

    private fun placeSpire() {
        for (y in shape.bottomY..shape.tipBaseY) {
            val centerX = shape.centerXAt(y)
            val centerZ = shape.centerZAt(y)
            val radius = shape.radiusAt(y)

            setBlock(Mth.floor(centerX), y, Mth.floor(centerZ), spireBlock(y))

            if (radius > 1.0f) {
                forEachInDisc(centerX, centerZ, radius) { x, z -> setBlock(x, y, z, spireBlock(y)) }
            }
        }
    }

    private fun placeCage() {
        val radius = CAGE_RADIUS
        val innerRadiusSqr = (radius - 0.5f) * (radius - 0.5f)
        val outerRadiusSqr = (radius + 0.5f) * (radius + 0.5f)

        for (dx in -radius..radius) {
            for (dy in -radius..radius) {
                for (dz in -radius..radius) {
                    val distanceSqr = (dx * dx + dy * dy + dz * dz).toFloat()
                    if (distanceSqr < innerRadiusSqr || distanceSqr > outerRadiusSqr) continue

                    pos.set(shape.tipX + dx, shape.crystalY + dy, shape.tipZ + dz)

                    if (level.getBlockState(pos).isAir) {
                        level.setBlock(pos, Blocks.IRON_BARS.defaultBlockState(), Block.UPDATE_ALL)
                    }
                }
            }
        }
    }

    private fun spawnCrystal(invulnerable: Boolean, beamTarget: BlockPos?) {
        val crystal = EntityTypes.END_CRYSTAL.create(level.level, EntitySpawnReason.STRUCTURE) ?: return

        crystal.setBeamTarget(beamTarget)
        crystal.setPermanentlyInvulnerable(invulnerable)
        crystal.setShowBottom(false)
        crystal.snapTo(shape.tipX + 0.5, shape.crystalY.toDouble(), shape.tipZ + 0.5, random.nextFloat() * 360.0F, 0.0F)

        level.addFreshEntity(crystal)
    }

    private fun spireBlock(y: Int): BlockState {
        val cryingChance = 0.2f * y / shape.tipBaseY
        return if (random.nextFloat() < cryingChance) Blocks.CRYING_OBSIDIAN.defaultBlockState() else Blocks.OBSIDIAN.defaultBlockState()
    }

    private inline fun forEachInDisc(centerX: Float, centerZ: Float, radius: Float, action: (Int, Int) -> Unit) {
        val radiusSqr = radius * radius

        for (x in floor(centerX - radius - 1).toInt()..ceil(centerX + radius + 1).toInt()) {
            for (z in floor(centerZ - radius - 1).toInt()..ceil(centerZ + radius + 1).toInt()) {
                val dx = (x + 0.5f) - centerX
                val dz = (z + 0.5f) - centerZ

                if (dx * dx + dz * dz <= radiusSqr) action(x, z)
            }
        }
    }

    private fun setBlock(x: Int, y: Int, z: Int, state: BlockState) {
        pos.set(x, y, z)
        level.setBlock(pos, state, Block.UPDATE_CLIENTS)
    }

    private companion object {
        const val CAGE_RADIUS = 4

        fun randomFor(worldSeed: Long, spike: EndSpikeFeature.EndSpike): RandomSource {
            val seed = worldSeed xor
                (spike.centerX * 341873128712L) xor
                (spike.centerZ * 132897987541L) xor
                (if (spike.isGuarded) 0x9E3779B97F4A7C15uL.toLong() else 0L)

            return RandomSource.create(seed)
        }
    }
}
