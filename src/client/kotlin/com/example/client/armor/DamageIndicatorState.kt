package com.example.client.armor

import net.minecraft.client.Minecraft

object DamageIndicatorState {
    data class Indicator(val worldYaw: Float, val creationTick: Long, val isHealthDamage: Boolean)

    private val indicators = mutableListOf<Indicator>()
    private var lastCapturedHurtTime = 0

    const val PERSISTENCE_TICKS = 60L

    fun tick() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val camera = mc.cameraEntity ?: return
        val level = mc.level ?: return
        val currentTime = level.gameTime

        if (player.hurtTime > 0 && player.hurtTime > lastCapturedHurtTime) {
            val worldYaw = player.hurtDir + camera.yRot + 180f
            val healthDamage = ClientArmorState.currentArmor <= 0f
            indicators.add(Indicator(worldYaw, currentTime, healthDamage))
            lastCapturedHurtTime = player.hurtTime
        }

        if (player.hurtTime <= 0) {
            lastCapturedHurtTime = 0
        }

        indicators.removeAll { currentTime - it.creationTick > PERSISTENCE_TICKS }
    }

    fun getIndicators(): List<Indicator> = indicators
}
