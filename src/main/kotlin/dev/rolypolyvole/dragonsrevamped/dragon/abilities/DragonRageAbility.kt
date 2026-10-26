package dev.rolypolyvole.dragonsrevamped.dragon.abilities

import dev.rolypolyvole.dragonsrevamped.dragon.DragonAbilityManager
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
    private var hasPerformed = false
    private var firstTick = true

    private val level = this.dragon.level() as ServerLevel
    private val center = Vec3(0.0, 128.0, 0.0)

    private val floatingBlocks = mutableListOf<FloatingBlock>()

    override fun isActive() = state != State.IDLE && state != State.DONE

    override fun onBeforeMove() {
        if (this.state == State.ASCENDING) {
            val direction = this.center.subtract(this.dragon.position())

            this.dragon.deltaMovement = if (direction.length() > 2.0) {
                direction.normalize().scale(1.6)
            } else {
                Vec3.ZERO
            }
        } else if (this.isActive()) {
            this.dragon.deltaMovement = Vec3.ZERO
        }
    }

    override fun tick() {
        when (state) {
            State.IDLE -> checkTrigger()
            State.ASCENDING -> tickAscending()
            State.GATHERING -> tickGathering()
            State.EXPLODING -> tickExploding()
            State.ROARING -> tickRoaring()
            State.DONE -> checkReset()
        }
    }

    private fun checkTrigger() {
        if (this.firstTick) {
            this.firstTick = false

            cleanUpBlockDisplays()

            if (this.dragon.health <= this.dragon.maxHealth * 0.5f) {
                this.hasPerformed = true
                transition(State.DONE)
            }

            return
        }

        if (this.hasPerformed) return

        if (this.dragon.health <= this.dragon.maxHealth * 0.5f) {
            transition(State.ASCENDING)
            this.dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)
        }
    }

    private fun checkReset() {
        if (this.dragon.health >= this.dragon.maxHealth) {
            this.hasPerformed = false
            this.state = State.IDLE
        }
    }

    private fun transition(newState: State) {
        this.state = newState
        this.stateTicks = 0
    }

    private fun tickAscending() {
        this.stateTicks++

        this.dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)

        if (this.stateTicks <= 20) {
            this.dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 1.5F - this.stateTicks * 0.05F)
        }

        val angle = this.stateTicks * 0.3
        val radius = 3.0

        this.manager.sendParticles(
            PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
            this.dragon.x + cos(angle) * radius, this.dragon.y + 1.0, this.dragon.z + sin(angle) * radius,
            5, 0.2, 0.5, 0.2, 0.0
        )

        if (this.stateTicks >= 80) {
            spawnBlockDisplays()
            transition(State.GATHERING)
        }
    }

    private fun tickGathering() {
        this.stateTicks++

        this.dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)

        val progress = this.stateTicks / 80.0
        val target = this.dragon.position().add(0.0, 2.0, 0.0)

        for (block in this.floatingBlocks) {
            val lerped = block.startPos.lerp(target, progress)
            block.display.teleportTo(lerped.x, lerped.y, lerped.z)
        }

        if (this.stateTicks % 20 == 0) {
            this.dragon.broadcastSound(SoundEvents.BEACON_ACTIVATE, pitch = 0.5F + progress.toFloat())
        }

        this.manager.sendParticles(
            ParticleTypes.REVERSE_PORTAL,
            this.dragon.x, this.dragon.y + 2.0, this.dragon.z,
            20, 3.0, 3.0, 3.0, 0.1
        )

        if (this.stateTicks >= 80) {
            transition(State.EXPLODING)
        }
    }

    private fun tickExploding() {
        val explosionCenter = this.dragon.position().add(0.0, 2.0, 0.0)

        this.level.explode(
            this.dragon, this.dragon.damageSources().dragonBreath(),
            null,
            explosionCenter.x, explosionCenter.y, explosionCenter.z,
            12.0F, false,
            Level.ExplosionInteraction.NONE
        )

        for (block in this.floatingBlocks) {
            val display = block.display
            display.discard()

            val direction = Vec3(
                1.0 - this.level.random.nextDouble() * 2.0,
                0.0,
                1.0 - this.level.random.nextDouble() * 2.0
            ).normalize()

            val speed = 1.0 + this.level.random.nextDouble() * 1.5
            val velocity = direction.scale(speed).add(0.0, 0.5 + this.level.random.nextDouble(), 0.0)

            ExplosionAnimation.spawnDebris(this.level, display.position(), block.blockState, velocity)
        }

        this.floatingBlocks.clear()

        this.dragon.broadcastSound(SoundEvents.LIGHTNING_BOLT_THUNDER, pitch = 0.5F)
        this.dragon.broadcastSound(SoundEvents.GENERIC_EXPLODE.value(), pitch = 0.5F)

        this.manager.sendParticles(
            ParticleTypes.EXPLOSION_EMITTER,
            explosionCenter.x, explosionCenter.y, explosionCenter.z,
            5, 2.0, 2.0, 2.0
        )

        transition(State.ROARING)
    }

    private fun tickRoaring() {
        this.stateTicks++

        this.dragon.phaseManager.setPhase(EnderDragonPhase.HOVERING)

        if (this.stateTicks <= 20) {
            this.dragon.broadcastSound(SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.5F + this.stateTicks * 0.05F)
        }

        if (this.stateTicks == 20) {
            respawnAllCrystals()

            EntityTypes.LIGHTNING_BOLT.create(this.level, EntitySpawnReason.MOB_SUMMONED)?.let {
                it.snapTo(this.dragon.x, this.dragon.y, this.dragon.z)
                it.setVisualOnly(true)
                this.level.addFreshEntity(it)
            }

            this.dragon.broadcastMessage(
                Component.empty()
                    .append(Component.literal("[Ender Dragon] ").withStyle(ChatFormatting.DARK_PURPLE))
                    .append(Component.literal("My strongest soldiers have arrived to end this battle!").withStyle(ChatFormatting.WHITE))
            )
        }

        if (this.stateTicks % 5 == 0) {
            this.manager.sendParticles(
                PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F),
                this.dragon.x, this.dragon.y + 1.0, this.dragon.z,
                30, 4.0, 2.0, 4.0
            )
        }

        if (this.stateTicks >= 80) {
            this.hasPerformed = true
            transition(State.DONE)
            this.dragon.phaseManager.setPhase(EnderDragonPhase.HOLDING_PATTERN)
        }
    }

    private fun spawnBlockDisplays() {
        val random = this.level.random

        repeat(150) {
            val roll = random.nextFloat()

            val state = when {
                roll > 0.35F -> Blocks.END_STONE.defaultBlockState()
                roll > 0.175F -> Blocks.OBSIDIAN.defaultBlockState()
                else -> Blocks.CRYING_OBSIDIAN.defaultBlockState()
            }

            val angle = random.nextDouble() * Math.PI * 2
            val distance = 30.0 + random.nextDouble() * 45.0
            val startPos = this.center.add(
                cos(angle) * distance,
                -20.0 + random.nextDouble() * 40.0,
                sin(angle) * distance
            )

            val display = Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, this.level)
            display.addTag(RAGE_BLOCK_TAG)
            display.blockState = state
            display.setPos(startPos.x, startPos.y, startPos.z)

            this.level.addFreshEntity(display)
            this.floatingBlocks.add(FloatingBlock(display, state, startPos))
        }
    }

    private fun cleanUpBlockDisplays() {
        val area = AABB.ofSize(this.center, 200.0, 200.0, 200.0)

        this.level.getEntitiesOfClass(Display.BlockDisplay::class.java, area)
            .filter { RAGE_BLOCK_TAG in it.entityTags() }
            .forEach { it.discard() }
    }

    private fun respawnAllCrystals() {
        for (pos in CustomEndSpikes.getCrystalLocations(this.level)) {
            val area = AABB.ofSize(pos, 3.0, 3.0, 3.0)

            this.level.getEntitiesOfClass(EndCrystal::class.java, area).forEach { it.discard() }
            this.level.getEntitiesOfClass(Skeleton::class.java, area).forEach { it.discard() }

            val crystal = EntityTypes.END_CRYSTAL.create(this.level, EntitySpawnReason.EVENT) ?: continue
            crystal.setPos(pos)
            crystal.setShowBottom(false)
            this.level.addFreshEntity(crystal)

            val protector = CrystalProtector(this.level)
            protector.equipArmor()
            protector.startRiding(crystal, true, false)

            this.level.addFreshEntity(protector)
        }

        this.manager.crystalRespawnAbility.resetCooldowns()
        this.manager.crystalRespawnAbility.sendCrystalCount()
    }

    companion object {
        private const val RAGE_BLOCK_TAG = "dragons_revamped_rage_block"
    }
}
