package dev.rolypolyvole.dragonsrevamped.util

import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

const val ARENA_RADIUS = 200.0

fun EnderDragon.isInArena(entity: Entity): Boolean =
    entity.position().xz().closerThan(Vec3.atCenterOf(fightOrigin).xz(), ARENA_RADIUS)

fun EnderDragon.arenaBounds(): AABB {
    val center = Vec3.atBottomCenterOf(fightOrigin)

    return AABB(
        center.x - ARENA_RADIUS, level().minY.toDouble(), center.z - ARENA_RADIUS,
        center.x + ARENA_RADIUS, level().maxY + 1.0, center.z + ARENA_RADIUS
    )
}

fun EnderDragon.podiumY(): Int =
    level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, fightOrigin).y

fun EnderDragon.nearbyPlayers(): List<ServerPlayer> =
    (level() as ServerLevel).players().filter { isInArena(it) }

fun EnderDragon.broadcastSound(sound: SoundEvent, volume: Float = 1.0F, pitch: Float = 1.0F) {
    nearbyPlayers().forEach { it.sendSound(sound, volume, pitch) }
}

fun EnderDragon.broadcastMessage(message: Component, actionBar: Boolean = false) {
    nearbyPlayers().forEach { it.sendSystemMessage(message, actionBar) }
}

fun ServerPlayer.sendSound(sound: SoundEvent, volume: Float = 1.0F, pitch: Float = 1.0F) {
    val packet = ClientboundSoundPacket(
        Holder.direct(sound), SoundSource.HOSTILE,
        x, y, z,
        volume, pitch, level().random.nextLong()
    )

    connection.send(packet)
}

fun BlockPos.highestBlockY(world: ServerLevel): BlockPos =
    world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this)

fun Vec3.xz(): Vec3 {
    return Vec3(x, 0.0, z)
}

@Suppress("DEPRECATION")
fun Entity.standingBlockPos(): BlockPos {
    val pos = blockPosition()
    if (level().getBlockState(pos.below()).isSolid) return pos

    return listOf(
        pos.north(), pos.south(), pos.east(), pos.west(),
        pos.north().east(), pos.north().west(),
        pos.south().east(), pos.south().west()
    ).firstOrNull { level().getBlockState(it.below()).isSolid } ?: pos
}
