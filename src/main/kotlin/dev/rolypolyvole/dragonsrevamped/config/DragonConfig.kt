package dev.rolypolyvole.dragonsrevamped.config

import net.minecraft.world.Difficulty
import kotlin.math.sqrt

class DragonConfig(
    val playerCount: Int = 1,
    val healthOverride: Double? = null,
    val maxCrystalProtectors: Int? = null,
    val disableFog: Boolean = true,
    val limitMaxDamagePerHit: Boolean = true,
    val dragonBreathErodesItems: Boolean = true,
    val hyperHeal: Boolean = true,
    val crystalHealRate: Double = 1.0,
    val hyperHealRate: Double = 1.0,
    val crystalProtectorSpawnRate: Double = 1.0,
    val crystalRespawnRate: Double = 1.0,
    val damage: DamageConfig = DamageConfig(),
    val deathMessages: DeathMessageConfig = DeathMessageConfig()
) {
    val fighters: Int
        get() = playerCount.coerceAtLeast(1)

    val maxHealth: Double
        get() = healthOverride ?: (800.0 + 700.0 * sqrt(fighters - 1.0))

    val crystalProtectorLimit: Int
        get() = maxCrystalProtectors ?: fighters
}

class DamageConfig(
    val headContact: DifficultyDamage = DifficultyDamage(8.0F, 14.0F, 21.0F),
    val wingContact: DifficultyDamage = DifficultyDamage(5.0F, 8.0F, 12.0F),
    val lightning: DifficultyDamage = DifficultyDamage(6.75F, 11.5F, 17.25F)
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

class DeathMessageConfig(
    val lightning: String = "<player> was struck down by <dragon>",
    val bodyHit: String = "<player> got run through by <dragon>"
)
