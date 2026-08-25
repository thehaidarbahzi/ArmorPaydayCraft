package com.example.client.armor

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.client.DeltaTracker
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement
import kotlin.math.cos
import kotlin.math.sin

object DamageIndicatorHud : HudElement {
    private val TEXTURE_ID = Identifier.fromNamespaceAndPath("template-mod", "textures/gui/damage_indicator.png")

    private const val ORBIT_DISTANCE = 30.0
    private const val TEXTURE_SIZE = 32

    override fun extractRenderState(graphics: GuiGraphicsExtractor, deltaTracker: DeltaTracker) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val camera = mc.cameraEntity ?: return

        DamageIndicatorState.tick()

        val indicators = DamageIndicatorState.getIndicators()
        if (indicators.isEmpty()) return

        val cx = graphics.guiWidth() / 2
        val cy = graphics.guiHeight() / 2
        val level = mc.level ?: return
        val currentTime = level.gameTime
        val halfSize = TEXTURE_SIZE / 2

        for (indicator in indicators) {
            val age = currentTime - indicator.creationTick
            val alpha = 1.0f - (age.toFloat() / DamageIndicatorState.PERSISTENCE_TICKS)
            if (alpha <= 0f) continue

            val screenAngleDeg = indicator.worldYaw - camera.yRot
            val screenAngleRad = Math.toRadians(screenAngleDeg.toDouble())

            val posX = cx + (cos(screenAngleRad) * ORBIT_DISTANCE).toFloat()
            val posY = cy + (sin(screenAngleRad) * ORBIT_DISTANCE).toFloat()

            val arrowDeg = screenAngleDeg - 90f
            val arrowRad = Math.toRadians(arrowDeg.toDouble()).toFloat()

            val alphaInt = (alpha * 255).toInt().coerceIn(0, 255)
            val rgb = if (indicator.isHealthDamage) 0xFF5555 else 0xFFFFFF
            val argb = (alphaInt shl 24) or rgb

            graphics.pose().pushMatrix()
            graphics.pose().translate(posX, posY)
            graphics.pose().rotate(arrowRad)
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE_ID,
                -halfSize, -halfSize,
                0f, 0f,
                TEXTURE_SIZE, TEXTURE_SIZE,
                TEXTURE_SIZE, TEXTURE_SIZE,
                argb
            )
            graphics.pose().popMatrix()
        }
    }
}
