package com.example.client.armor

import net.minecraft.client.Minecraft

object ClientArmorState {
    var currentArmor: Float = 0f
    var maxArmor: Float = 0f
    var isRecovering: Boolean = false
    var recoveryProgress: Float = 0f
    var hitAnimationTime: Long = 0L
    var isHealthDamage: Boolean = false

    private var lastSyncedArmor: Float = 0f
    var isBlinking: Boolean = false
        private set
    var isShaking: Boolean = false
        private set

    fun updateFromServer(current: Float, max: Float, recovering: Boolean, progress: Float, hitTime: Long, healthDamage: Boolean) {
        if (current < lastSyncedArmor && lastSyncedArmor > 0f) {
            isBlinking = true
        }

        currentArmor = current
        maxArmor = max
        isRecovering = recovering
        recoveryProgress = progress
        hitAnimationTime = hitTime
        isHealthDamage = healthDamage

        lastSyncedArmor = current
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = player.level() ?: return
        val currentTime = level.gameTime

        // Blink lasts ~20 ticks (1 second) after hit
        if (isBlinking && currentTime - hitAnimationTime > 20L) {
            isBlinking = false
        }

        // Shake when armor is low (<25% of max) or empty (0)
        isShaking = maxArmor > 0f && (currentArmor <= 0f || (currentArmor / maxArmor) < 0.25f)
    }

    fun hasArmor(): Boolean = maxArmor > 0f

    fun getArmorPercentage(): Float {
        return if (maxArmor > 0) (currentArmor / maxArmor).coerceIn(0f, 1f) else 0f
    }
}
