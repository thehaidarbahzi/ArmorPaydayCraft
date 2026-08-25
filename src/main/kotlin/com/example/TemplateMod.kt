package com.example

import com.example.armor.ArmorManager
import com.example.armor.ArmorNetwork
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object TemplateMod : ModInitializer {
	const val MOD_ID: String = "template-mod"

	private val LOGGER = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		LOGGER.info("Initializing Payday 2 Armor System...")

		// Register network payload type (clientbound = server -> client)
		PayloadTypeRegistry.clientboundPlay().register(ArmorNetwork.SYNC_TYPE, ArmorNetwork.SYNC_CODEC)

		// Register tick handler for armor recovery
		ServerTickEvents.END_SERVER_TICK.register { server ->
			for (player in server.playerList.players) {
				ArmorManager.tick(player)
			}
		}

		// Register player join/disconnect events
		ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
			val player = handler.player
			ArmorManager.onPlayerJoin(player)
		}

		ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
			val player = handler.player
			ArmorManager.onPlayerDisconnect(player)
		}

		LOGGER.info("Payday 2 Armor System initialized!")
	}

	fun id(path: String): Identifier
		= Identifier.fromNamespaceAndPath(MOD_ID, path)
}
