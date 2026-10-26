package dev.rolypolyvole.dragonsrevamped.config

import net.minecraft.world.Difficulty
import kotlin.math.sqrt

class DragonConfig(
    val playerCount: Int = 1,
    val dragon: DragonSettings = DragonSettings(),
    val healing: HealingConfig = HealingConfig(),
    val crystals: CrystalConfig = CrystalConfig(),
    val crystalProtectors: CrystalProtectorConfig = CrystalProtectorConfig(),
    val damage: DamageConfig = DamageConfig(),
    val misc: MiscConfig = MiscConfig(),
    val deathMessages: DeathMessageConfig = DeathMessageConfig()
) {
    val fighters: Int
        get() = playerCount.coerceAtLeast(1)

    val maxHealth: Double
        get() = dragon.healthOverride ?: (800.0 + 700.0 * sqrt(fighters - 1.0))

    val crystalProtectorLimit: Int
        get() = crystalProtectors.maxAlive ?: fighters
}

class DragonSettings(
    val healthOverride: Double? = null,
    val gracePeriodSeconds: Int = 15,
    val speedMultiplier: Double = 0.8,
    val diveSpeedMultiplier: Double = 1.0,
    val limitMaxDamagePerHit: Boolean = true,
    val maxDamagePerHit: Double = 200.0
)

class HealingConfig(
    val crystalHealRate: Double = 1.0,
    val hyperHeal: Boolean = true,
    val hyperHealRate: Double = 1.0
)

class CrystalConfig(
    val respawnRate: Double = 1.0,
    val respawnThreshold: Int = 7,
    val initialDamage: Double = 50.0,
    val respawnedDamage: Double = 30.0
)

class CrystalProtectorConfig(
    val maxAlive: Int? = null,
    val spawnRate: Double = 1.0,
    val amountOnCrystals: Int = 2
)

class DamageConfig(
    val headContact: DifficultyDamage = DifficultyDamage(9.0F, 16.0F, 24.0F),
    val wingContact: DifficultyDamage = DifficultyDamage(6.5F, 11.0F, 16.5F),
    val lightning: DifficultyDamage = DifficultyDamage(6.75F, 11.5F, 17.25F),
    val protectionEffectiveness: Double = 0.5
)

class DifficultyDamage(
    val easy: Float = 0.0F,
    val normal: Float = 0.0F,
    val hard: Float = 0.0F
) {
    fun forDifficulty(difficulty: Difficulty): Float = when (difficulty) {
        Difficulty.PEACEFUL, Difficulty.EASY -> easy
        Difficulty.NORMAL -> normal
        Difficulty.HARD -> hard
    }
}

class MiscConfig(
    val disableFog: Boolean = true,
    val dragonBreathErodesItems: Boolean = true,
    val endermiteChance: Double = 0.5
)

class DeathMessageConfig(
    val lightning: String = "<player> was struck down by <dragon>",
    val bodyHit: String = "<player> got run through by <dragon>"
)
