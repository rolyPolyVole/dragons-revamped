package dev.rolypolyvole.dragonsrevamped.worldgen

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ServerLevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.Half
import net.minecraft.world.level.block.state.properties.SlabType

object TipStructure {
    const val HEIGHT = 6

    private val centerColumn = listOf(
        Blocks.OBSIDIAN,
        Blocks.OBSIDIAN,
        Blocks.CRYING_OBSIDIAN,
        Blocks.REINFORCED_DEEPSLATE,
        Blocks.BEDROCK,
        Blocks.SOUL_FIRE
    )

    private val corners = listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)

    private val candles: BlockState = Blocks.DYED_CANDLE.black().defaultBlockState()
        .setValue(BlockStateProperties.LIT, false)
        .setValue(BlockStateProperties.CANDLES, 4)

    private val topSlab: BlockState = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
        .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP)

    private val bottomSlab: BlockState = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
        .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM)

    fun place(level: ServerLevelAccessor, x: Int, baseY: Int, z: Int) {
        centerColumn.forEachIndexed { index, block ->
            setBlock(level, x, baseY + index + 1, z, block.defaultBlockState())
        }

        for (direction in Direction.Plane.HORIZONTAL) {
            val sideX = x + direction.stepX
            val sideZ = z + direction.stepZ

            setBlock(level, sideX, baseY + 4, sideZ, upsideDownStair(direction.opposite))
            setBlock(level, sideX, baseY + 5, sideZ, candles)
        }

        for ((dx, dz) in corners) {
            setBlock(level, x + dx, baseY + 4, z + dz, topSlab)
            setBlock(level, x + dx, baseY + 5, z + dz, bottomSlab)
        }
    }

    fun isBuilt(level: BlockGetter, x: Int, crystalY: Int, z: Int): Boolean =
        level.getBlockState(BlockPos(x, crystalY - 1, z)).`is`(Blocks.BEDROCK)

    private fun upsideDownStair(facing: Direction): BlockState = Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState()
        .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
        .setValue(BlockStateProperties.HALF, Half.TOP)

    private fun setBlock(level: ServerLevelAccessor, x: Int, y: Int, z: Int, state: BlockState) {
        level.setBlock(BlockPos(x, y, z), state, Block.UPDATE_CLIENTS)
    }
}
