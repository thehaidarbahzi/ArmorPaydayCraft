package com.example.armor

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ArmorManager {
    private val playerStates = ConcurrentHashMap<UUID, PlayerArmorState>()
    private const val RECOVERY_DELAY_TICKS = 50L
    private const val RECOVERY_DURATION_TICKS = 10L

    data class PlayerArmorState(
        var currentArmor: Float = 0f,
        var maxArmor: Float = 0f,
        var lastHitTime: Long = 0L,
        var isRecovering: Boolean = false,
        var recoveryProgress: Float = 0f,
        var recoveryStartTime: Long = 0L,
        var recoveryStartArmor: Float = 0f,
        var hitAnimationTime: Long = 0L,
        var lastHitHealthDamage: Boolean = false
    )

    fun getState(player: Player): PlayerArmorState {
        return playerStates.getOrPut(player.uuid) { PlayerArmorState() }
    }

    private fun calculateMaxArmor(player: Player): Float {
        return player.getArmorValue().toFloat()
    }

    private fun getDepletion(state: PlayerArmorState): Float {
        if (state.maxArmor <= 0f) return 0f
        return (1f - (state.currentArmor / state.maxArmor)).coerceIn(0f, 1f)
    }

    private fun handleArmorChange(state: PlayerArmorState, player: Player): Boolean {
        val newMax = calculateMaxArmor(player)
        if (newMax == state.maxArmor) return false

        val oldDepletion = getDepletion(state)
        state.maxArmor = newMax

        if (newMax > 0f) {
            state.currentArmor = newMax * (1f - oldDepletion)
            if (oldDepletion >= 1f) {
                state.currentArmor = 0f
                state.lastHitTime = player.level().gameTime
            }
        } else {
            state.currentArmor = 0f
        }
        syncToClient(player)
        return true
    }

    fun absorbDamage(player: Player, damage: Float): Float {
        val state = getState(player)
        handleArmorChange(state, player)

        if (state.isRecovering) {
            state.isRecovering = false
            state.recoveryProgress = 0f
        }

        state.lastHitTime = player.level().gameTime
        state.hitAnimationTime = player.level().gameTime

        if (state.currentArmor > 0f) {
            val absorbed = minOf(damage, state.currentArmor)
            state.currentArmor -= absorbed
            state.lastHitHealthDamage = false
            syncToClient(player)
            return 0f
        }

        state.lastHitHealthDamage = true
        return damage
    }

    fun tick(player: Player) {
        val state = getState(player)
        handleArmorChange(state, player)

        if (state.maxArmor <= 0f) {
            if (state.currentArmor != 0f || state.isRecovering) {
                state.currentArmor = 0f
                state.isRecovering = false
                state.recoveryProgress = 0f
                syncToClient(player)
            }
            return
        }

        if (state.currentArmor >= state.maxArmor && !state.isRecovering) return

        val currentTime = player.level().gameTime
        val ticksSinceLastHit = currentTime - state.lastHitTime

        if (!state.isRecovering && ticksSinceLastHit >= RECOVERY_DELAY_TICKS) {
            state.isRecovering = true
            state.recoveryStartTime = currentTime
            state.recoveryStartArmor = state.currentArmor
            state.recoveryProgress = 0f
        }

        if (state.isRecovering) {
            val recoveryTicks = currentTime - state.recoveryStartTime
            val newProgress = (recoveryTicks.toFloat() / RECOVERY_DURATION_TICKS).coerceIn(0f, 1f)

            if (newProgress != state.recoveryProgress) {
                state.recoveryProgress = newProgress
                val range = state.maxArmor - state.recoveryStartArmor
                state.currentArmor = state.recoveryStartArmor + range * state.recoveryProgress

                if (state.recoveryProgress >= 1f) {
                    state.currentArmor = state.maxArmor
                    state.isRecovering = false
                    state.recoveryProgress = 0f
                }

                syncToClient(player)
            }
        }
    }

    fun onPlayerJoin(player: Player) {
        val state = getState(player)
        state.maxArmor = calculateMaxArmor(player)
        state.currentArmor = state.maxArmor
        state.lastHitTime = player.level().gameTime
        state.isRecovering = false
        state.recoveryProgress = 0f
        syncToClient(player)
    }

    fun onPlayerDisconnect(player: Player) {
        playerStates.remove(player.uuid)
    }

    private fun syncToClient(player: Player) {
        if (player is ServerPlayer) {
            ArmorNetwork.sendToPlayer(player)
        }
    }
}
