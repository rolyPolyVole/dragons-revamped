package dev.rolypolyvole.dragonsrevamped.dragon.abilities

import dev.rolypolyvole.dragonsrevamped.config.ModConfig
import dev.rolypolyvole.dragonsrevamped.dragon.Crystals
import dev.rolypolyvole.dragonsrevamped.dragon.DragonAbilityManager
import dev.rolypolyvole.dragonsrevamped.dragon.SPEED_MULTIPLIER
import dev.rolypolyvole.dragonsrevamped.dragon.entities.CrystalProtector
import dev.rolypolyvole.dragonsrevamped.util.ExplosionAnimation
import dev.rolypolyvole.dragonsrevamped.util.broadcastMessage
import dev.rolypolyvole.dragonsrevamped.util.broadcastSound
import dev.rolypolyvole.dragonsrevamped.worldgen.CustomEndSpikes
import net.minecraft.ChatFormatting
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.PowerParticleOption
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.boss.enderdragon.EnderDragon
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase
import net.minecraft.world.entity.monster.skeleton.Skeleton
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

class DragonRageAbility(
    dragon: EnderDragon,
    manager: DragonAbilityManager
) : AbstractDragonAbility(dragon, manager) {

    enum class State {
        IDLE, ASCENDING, GATHERING, EXPLODING, ROARING, DONE
    }

    private data class FloatingBlock(
        val display: Display.BlockDisplay,
        val blockState: BlockState,
        val startPos: Vec3
    )

    var state = State.IDLE
        private set

    private var stateTicks = 0
    var hasPerformed = false
        private set
    private var firstTick = true

    private val level = dragon.level() as ServerLevel
    private val center = Vec3(0.0, 128.0, 0.0)

    private val floatingBlocks = mutableListOf<FloatingBlock>()

    override fun isActive() = state != State.IDLE && state != State.DONE

    override fun onBeforeMove() {
        if (state == State.ASCENDING) {
            val direction = center.subtract(dragon.position())

            dragon.deltaMovement = if (direction.length() > 2.0) {
                direction.normalize().scale(1.6 * SPEED_MULTIPLIER)
            } else {
                Vec3.ZERO
            }
        } else if (isActive()) {
            dragon.deltaMovement = Vec3.ZERO
        }
    }

    override fun tick() {
        when (state) {
            State.IDLE -> checkTrigger()
            State.ASCENDING -> tickAscending()
            State.GATHERING -> tickGathering()
            State.EXPLODING -> tickExploding()
            State.ROARING -> tickRoaring()
            State.DONE -> {}
        }
    }

    private fun checkTrigger() {
        if (firstTick) {
            firstTick = false

            cleanBlockDisplays()

            if (dragon.health <= dragon.maxHealth * 0.5f) {
                hasPerformed = true
                transition(State.DONE)
            }

            return
        }

        if (hasPerformed) return

        if (dragon.health <= dragon.maxHealth * 0.5f) {
            transition(State.ASCENDING)
            dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)
        }
    }

    private fun transition(newState: State) {
        state = newState
        stateTicks = 0
    }

    private fun tickAscending() {
        stateTicks++

        dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)

        if (stateTicks <= 20) {
            dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.5F - stateTicks * 0.05F)
        }

        val angle = stateTicks * 0.3
        val radius = 3.0

        manager.sendParticles(
            PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
            dragon.x + cos(angle) * radius, dragon.y + 1.0, dragon.z + sin(angle) * radius,
            5, 0.2, 0.5, 0.2, 0.0
        )

        if (stateTicks >= 80) {
            spawnBlockDisplays()
            transition(State.GATHERING)
        }
    }

    private fun tickGathering() {
        stateTicks++

        dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)

        val progress = stateTicks / 80.0
        val target = dragon.position().add(0.0, 2.0, 0.0)

        for ((display, _, startPos) in floatingBlocks) {
            val lerped = startPos.lerp(target, progress)
            display.teleportTo(lerped.x, lerped.y, lerped.z)
        }

        if (stateTicks % 20 == 0) {
            dragon.broadcastSound(SoundEvents.BEACON_ACTIVATE, pitch = 0.5F + progress.toFloat())
        }

        manager.sendParticles(
            ParticleTypes.REVERSE_PORTAL,
            dragon.x, dragon.y + 2.0, dragon.z,
            20, 3.0, 3.0, 3.0, 0.1
        )

        if (stateTicks >= 80) {
            transition(State.EXPLODING)
        }
    }

    private fun tickExploding() {
        val explosionCenter = dragon.position().add(0.0, 2.0, 0.0)

        level.explode(
            dragon, dragon.damageSources().dragonBreath(),
            null,
            explosionCenter.x, explosionCenter.y, explosionCenter.z,
            12.0F, false,
            Level.ExplosionInteraction.NONE
        )

        for ((display, blockState) in floatingBlocks) {
            display.discard()

            val direction = Vec3(
                1.0 - level.random.nextDouble() * 2.0,
                0.0,
                1.0 - level.random.nextDouble() * 2.0
            ).normalize()

            val speed = 1.0 + level.random.nextDouble() * 1.5
            val velocity = direction.scale(speed).add(0.0, 0.5 + level.random.nextDouble(), 0.0)

            ExplosionAnimation.spawnDebris(level, display.position(), blockState, velocity)
        }

        floatingBlocks.clear()

        dragon.broadcastSound(SoundEvents.LIGHTNING_BOLT_THUNDER, pitch = 0.5F)
        dragon.broadcastSound(SoundEvents.GENERIC_EXPLODE.value(), pitch = 0.5F)

        manager.sendParticles(
            ParticleTypes.EXPLOSION_EMITTER,
            explosionCenter.x, explosionCenter.y, explosionCenter.z,
            5, 2.0, 2.0, 2.0
        )

        transition(State.ROARING)
    }

    private fun tickRoaring() {
        stateTicks++

        dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)

        if (stateTicks <= 20) {
            dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.5F + stateTicks * 0.05F)
        }

        if (stateTicks == 20) {
            respawnAllCrystals()

            EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.MOB_SUMMONED)?.let {
                it.snapTo(dragon.x, dragon.y, dragon.z)
                it.setVisualOnly(true)
                level.addFreshEntity(it)
            }

            dragon.broadcastMessage(
                Component.empty()
                    .append(Component.literal("[Ender Dragon] ").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal("My strongest soldiers have arrived to end this battle!").withStyle(ChatFormatting.WHITE))
            )
        }

        if (stateTicks % 5 == 0) {
            manager.sendParticles(
                PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                dragon.x, dragon.y + 1.0, dragon.z,
                30, 4.0, 2.0, 4.0
            )
        }

        if (stateTicks >= 80) {
            hasPerformed = true
            transition(State.DONE)
            dragon.phaseManager.setPhase(EnderDragonPhase.HOLDING_PATTERN)
        }
    }

    private fun spawnBlockDisplays() {
        val random = level.random

        repeat(150) {
            val roll = random.nextFloat()

            val state = when {
                roll > 0.35F -> Blocks.END_STONE.defaultBlockState()
                roll > 0.175F -> Blocks.OBSIDIAN.defaultBlockState()
                else -> Blocks.CRYING_OBSIDIAN.defaultBlockState()
            }

            val angle = random.nextDouble() * Math.PI * 2
            val distance = 30.0 + random.nextDouble() * 45.0
            val startPos = center.add(
                cos(angle) * distance,
                -20.0 + random.nextDouble() * 40.0,
                sin(angle) * distance
            )

            val display = Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level)
            display.addTag(RAGE_BLOCK_TAG)
            display.blockState = state
            display.setPos(startPos.x, startPos.y, startPos.z)

            level.addFreshEntity(display)
            floatingBlocks.add(FloatingBlock(display, state, startPos))
        }
    }

    private fun cleanBlockDisplays() {
        val area = AABB.ofSize(center, 200.0, 200.0, 200.0)

        level.getEntitiesOfClass(Display.BlockDisplay::class.java, area)
            .filter { RAGE_BLOCK_TAG in it.entityTags() }
            .forEach { it.discard() }
    }

    private fun respawnAllCrystals() {
        val sites = CustomEndSpikes.getCrystalSites(level).shuffled().sortedByDescending { it.guarded }
        val protectedSites = sites.take(ModConfig.current.fighters * 2)

        for (site in sites) {
            val area = AABB.ofSize(site.position, 3.0, 3.0, 3.0)

            level.getEntitiesOfClass(EndCrystal::class.java, area).forEach { it.discard() }
            level.getEntitiesOfClass(Skeleton::class.java, area).forEach { it.discard() }

            val crystal = Crystals.spawnRespawned(level, site.position) ?: continue
            if (site !in protectedSites) continue

            val protector = CrystalProtector(level)
            protector.equipArmor()
            protector.startRiding(crystal, true, false)

            level.addFreshEntity(protector)
        }

        manager.crystalRespawnAbility.resetCooldowns()
        manager.crystalRespawnAbility.sendCrystalCount()
    }

    companion object {
        private const val RAGE_BLOCK_TAG = "dragons_revamped_rage_block"
    }
}
