package com.example.client

import com.example.armor.ArmorNetwork
import com.example.client.armor.ClientArmorState
import com.example.client.armor.DamageIndicatorHud
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object TemplateModClient : ClientModInitializer {
	private val LOGGER = LoggerFactory.getLogger("template-mod")

	override fun onInitializeClient() {
		LOGGER.info("Initializing client-side armor system...")

		// Register receiver for armor sync packets
		ClientPlayNetworking.registerGlobalReceiver(ArmorNetwork.SYNC_TYPE) { payload, _ ->
			ClientArmorState.updateFromServer(
				payload.currentArmor,
				payload.maxArmor,
				payload.isRecovering,
				payload.recoveryProgress,
				payload.hitAnimationTime,
				payload.isHealthDamage
			)
		}

		// Tick client armor state every frame for animations
		ClientTickEvents.END_CLIENT_TICK.register { ClientArmorState.tick() }

		// Register damage indicator HUD element after crosshair
		HudElementRegistry.attachElementAfter(
			VanillaHudElements.CROSSHAIR,
			Identifier.fromNamespaceAndPath("template-mod", "damage_indicator"),
			DamageIndicatorHud
		)
	}
}
