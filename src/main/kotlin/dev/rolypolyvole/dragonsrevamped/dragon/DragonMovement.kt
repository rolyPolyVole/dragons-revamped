package dev.rolypolyvole.dragonsrevamped.dragon

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.util.nearbyPlayers
import dev.rolypolyvole.dragonsrevamped.util.podiumY
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.TicketType
import net.minecraft.util.Mth
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2

private const val MAX_ARENA_DISTANCE = 150.0
private const val CHUNK_TICKET_INTERVAL = 10
private const val CHUNK_TICKET_RADIUS = 3
private const val MAX_DEPTH_BELOW_PODIUM = 25

fun EnderDragon.flyTowards(target: Vec3, speed: Double) {
    deltaMovement = target.subtract(position()).normalize().scale(speed * ModConfig.current.dragon.speedMultiplier)
}

fun EnderDragon.turnTowards(target: Vec3, maxStep: Float) {
    val dx = target.x - x
    val dz = target.z - z
    val targetYaw = 180.0F - (atan2(dx, dz) * Mth.RAD_TO_DEG).toFloat()

    yRot = Mth.approachDegrees(yRot, targetYaw, maxStep)
}

fun EnderDragon.keepChunksTicking() {
    if (tickCount % CHUNK_TICKET_INTERVAL != 0 || nearbyPlayers().isEmpty()) return

    (level() as ServerLevel).chunkSource.addTicketWithRadius(TicketType.ENDER_PEARL, chunkPosition(), CHUNK_TICKET_RADIUS)
}

fun EnderDragon.arenaCenter(height: Double): Vec3 = Vec3.atBottomCenterOf(fightOrigin).add(0.0, height, 0.0)

fun EnderDragon.isOffside(): Boolean {
    val horizontalDistance = position().multiply(1.0, 0.0, 1.0).distanceTo(arenaCenter(0.0).multiply(1.0, 0.0, 1.0))
    return horizontalDistance > MAX_ARENA_DISTANCE || y < podiumY() - MAX_DEPTH_BELOW_PODIUM
}
