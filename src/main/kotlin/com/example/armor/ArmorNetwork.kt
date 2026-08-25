package com.example.armor

import com.example.TemplateMod
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.level.ServerPlayer

object ArmorNetwork {
    val SYNC_TYPE = CustomPacketPayload.createType<ArmorSyncPayload>(
        "${TemplateMod.MOD_ID}/armor_sync"
    )

    val SYNC_CODEC: StreamCodec<FriendlyByteBuf, ArmorSyncPayload> = StreamCodec.of(
        { buf, payload ->
            buf.writeFloat(payload.currentArmor)
            buf.writeFloat(payload.maxArmor)
            buf.writeBoolean(payload.isRecovering)
            buf.writeFloat(payload.recoveryProgress)
            buf.writeLong(payload.hitAnimationTime)
            buf.writeBoolean(payload.isHealthDamage)
        },
        { buf ->
            ArmorSyncPayload(
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readFloat(),
                buf.readLong(),
                buf.readBoolean()
            )
        }
    )

    data class ArmorSyncPayload(
        val currentArmor: Float,
        val maxArmor: Float,
        val isRecovering: Boolean,
        val recoveryProgress: Float,
        val hitAnimationTime: Long,
        val isHealthDamage: Boolean
    ) : CustomPacketPayload {
        override fun type(): CustomPacketPayload.Type<ArmorSyncPayload> = SYNC_TYPE
    }

    fun sendToPlayer(player: ServerPlayer) {
        val state = ArmorManager.getState(player)
        val payload = ArmorSyncPayload(
            currentArmor = state.currentArmor,
            maxArmor = state.maxArmor,
            isRecovering = state.isRecovering,
            recoveryProgress = state.recoveryProgress,
            hitAnimationTime = state.hitAnimationTime,
            isHealthDamage = state.lastHitHealthDamage
        )
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload)
    }

    fun sendToAllPlayers(server: net.minecraft.server.MinecraftServer) {
        for (player in server.playerList.players) {
            sendToPlayer(player)
        }
    }
}
