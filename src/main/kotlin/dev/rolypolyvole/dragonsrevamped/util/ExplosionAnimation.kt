package dev.rolypolyvole.dragonsrevamped.util

import dev.rolypolyvole.dragonsrevamped.mixin.accessor.FallingBlockEntityInvoker
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3

object ExplosionAnimation {

    fun play(level: ServerLevel, blocks: List<Pair<BlockPos, BlockState>>, scalar: Double) {
        val destroyed = blocks.filter { (pos, state) -> !state.isAir && level.getBlockState(pos).isAir }
        val selected = destroyed.shuffled().take(15)

        for ((pos, state) in selected) {
            val x = -1.0 + level.random.nextDouble() * 2.0
            val y = 0.5 + level.random.nextDouble() * 1.5
            val z = -1.0 + level.random.nextDouble() * 2.0
            val velocity = Vec3(x, y, z).normalize().scale(scalar)

            spawnDebris(level, Vec3.atBottomCenterOf(pos), state, velocity)
        }
    }

    fun spawnDebris(level: ServerLevel, position: Vec3, state: BlockState, velocity: Vec3) {
        val blockState = if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            state.setValue(BlockStateProperties.WATERLOGGED, false)
        } else {
            state
        }

        val debris = FallingBlockEntityInvoker.create(level, position.x, position.y, position.z, blockState)
        debris.deltaMovement = velocity
        debris.dropItem = false
        debris.disableDrop()

        level.addFreshEntity(debris)
    }
}
