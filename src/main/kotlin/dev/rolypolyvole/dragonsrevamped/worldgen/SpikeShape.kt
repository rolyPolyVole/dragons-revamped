package dev.rolypolyvole.dragonsrevamped.worldgen

import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature
import kotlin.math.max
import kotlin.math.pow

class SpikeShape(spike: EndSpikeFeature.EndSpike, minY: Int, random: RandomSource) {
    val tipX = spike.centerX
    val tipZ = spike.centerZ
    val crystalY = spike.height
    val tipBaseY = crystalY - TipStructure.HEIGHT
    val bottomY = max(minY, tipBaseY - SPIRE_HEIGHT)
    val baseRadius = Mth.clamp(spireHeight() / 10 + 2, 4, 9)

    private val leanX: Float
    private val leanZ: Float

    init {
        val length = Mth.sqrt((tipX * tipX + tipZ * tipZ).toFloat())
        val outwardX = if (length == 0f) 0f else tipX / length
        val outwardZ = if (length == 0f) 0f else tipZ / length
        val wobble = (random.nextFloat() - 0.5f) * (MAX_LEAN_PER_Y * 0.25f)

        leanX = outwardX * MAX_LEAN_PER_Y - outwardZ * wobble
        leanZ = outwardZ * MAX_LEAN_PER_Y + outwardX * wobble
    }

    fun centerXAt(y: Int): Float = tipX - (tipBaseY - y) * leanX

    fun centerZAt(y: Int): Float = tipZ - (tipBaseY - y) * leanZ

    fun radiusAt(y: Int): Float {
        val progress = (y - bottomY) / spireHeight().toFloat()
        return 0.5f + (baseRadius - 0.5f) * (1.0f - progress).pow(TAPER_EXPONENT)
    }

    private fun spireHeight(): Int = max(1, tipBaseY - bottomY)

    private companion object {
        const val SPIRE_HEIGHT = 80
        const val MAX_LEAN_PER_Y = 0.35f
        const val TAPER_EXPONENT = 0.97f
    }
}
