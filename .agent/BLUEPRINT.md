# Blueprint: Payday 2-Style Armor System Mod

## Overview

A Minecraft Fabric mod that changes how armor works to follow Payday 2's armor mechanics:
- **Armor absorbs ALL damage** when armor points remain (even 1 point absorbs everything)
- **Armor depletes** when absorbing damage (each hit reduces armor points)
- **Health takes damage** only when armor reaches 0
- **Armor regenerates** after 2.5 seconds of no damage, over 0.5 seconds
- **Armor swap** carries depletion percentage
- **Damage animations** — blink flash + low armor shake
- **Damage indicator** — directional arrow around crosshair (white=armor, red=health)

## Architecture

### Files

#### Server-Side (src/main/)

1. **`ArmorManager.kt`** — Armor state tracking, absorption, recovery, depletion carryover
2. **`ArmorNetwork.kt`** — Network packet definition and sending
3. **`LivingEntityArmorMixin.java`** — Intercepts `getDamageAfterArmorAbsorb` at HEAD
4. **`TemplateMod.kt`** — Entrypoint

#### Client-Side (src/client/)

5. **`ClientArmorState.kt`** — Received armor state, blink/shake animation
6. **`DamageIndicatorState.kt`** — Active damage indicators (direction, lifetime, type)
7. **`DamageIndicatorHud.kt`** — Renders directional indicator with rotation + fade
8. **`ArmorBarMixin.java`** — Custom armor bar HUD
9. **`TemplateModClient.kt`** — Client entrypoint

### Data Flow

```
Server: Player takes damage
  → LivingEntityArmorMixin intercepts getDamageAfterArmorAbsorb
  → ArmorManager.absorbDamage() reduces armor
  → If armor absorbs all → return 0 (no health damage)
  → If armor depleted → return damage, set isHealthDamage=true
  → ArmorNetwork.sendToPlayer() syncs state to client

Server: Every tick
  → ArmorManager.tick() checks recovery + armor changes
  → Syncs on state change

Client: Receives packet
  → ClientArmorState updated
  → ClientArmorState.tick() updates blink/shake
  → ArmorBarMixin renders HUD

Client: Player takes damage (hurtTime > 0)
  → DamageIndicatorState captures direction from Player.getHurtDir()
  → DamageIndicatorHud renders indicator with rotation + alpha fade
```

### State Management

```kotlin
// Server-side
data class PlayerArmorState(
    var currentArmor: Float,
    var maxArmor: Float,
    var lastHitTime: Long,
    var isRecovering: Boolean,
    var recoveryProgress: Float,
    var recoveryStartTime: Long,
    var recoveryStartArmor: Float,
    var hitAnimationTime: Long,
    var lastHitHealthDamage: Boolean
)

// Client-side
object ClientArmorState {
    var currentArmor: Float
    var maxArmor: Float
    var isRecovering: Boolean
    var recoveryProgress: Float
    var hitAnimationTime: Long
    var isHealthDamage: Boolean
    var isBlinking: Boolean
    var isShaking: Boolean
}

object DamageIndicatorState {
    data class Indicator(val worldYaw: Float, val creationTick: Long, val isHealthDamage: Boolean)
}
```

### Constants

```kotlin
RECOVERY_DELAY_TICKS = 50L       // 2.5 seconds at 20 TPS
RECOVERY_DURATION_TICKS = 10L     // 0.5 seconds at 20 TPS
PERSISTENCE_TICKS = 60L           // 3 seconds indicator lifetime
ORBIT_DISTANCE = 30.0             // pixels from crosshair
TEXTURE_SIZE = 32                 // indicator size in GUI pixels
```

### Mixin Injection Points
- **`LivingEntity.getDamageAfterArmorAbsorb(DamageSource, float)`**: HEAD — cancels when armor absorbs all
- **`Hud.extractArmor(...)`**: HEAD — replaces vanilla armor bar
- **`ClientPlayNetworkHandler.onDamageTilt(DamageTiltS2CPacket)`**: NOT USED — MC 26.2 removed this packet. Direction captured via `Player.getHurtDir()` instead.

### Network Protocol
- Packet: `ArmorSyncPayload` with fields: `currentArmor`, `maxArmor`, `isRecovering`, `recoveryProgress`, `hitAnimationTime`, `isHealthDamage`
- Registered via `PayloadTypeRegistry.clientboundPlay()`

## What This Mod Does NOT Change

- Vanilla armor values
- Armor durability
- Enchantments
- Armor trimming
- Armor stands
- Mob armor

## Implementation Status

### Completed
- [x] Server-side armor absorption (DSOD-style)
- [x] Armor state tracking per player
- [x] Recovery system (2.5s delay + 0.5s smooth fill)
- [x] Armor swap depletion carryover
- [x] Server→client sync
- [x] Custom armor bar HUD
- [x] Blink + shake animations
- [x] Directional damage indicator (orbit, rotation, alpha fade)
- [x] Color-coded indicator (white=armor, red=health)
- [x] GUI-scale aware sizing
- [x] Player join/leave state management

### Not Implemented (Future)
- [ ] Armor-piercing mechanic (PD2 sniper)
- [ ] Sound effects
- [ ] Config file for tuning values
