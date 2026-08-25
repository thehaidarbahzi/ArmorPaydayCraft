package com.example.client

import com.example.armor.ArmorNetwork
import com.example.client.armor.ClientArmorState
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
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
				payload.hitAnimationTime
			)
		}

		// Tick client armor state every frame for animations
		ClientTickEvents.END_CLIENT_TICK.register { ClientArmorState.tick() }
	}
}
