# Blueprint: Payday 2-Style Armor System Mod

## Overview

A Minecraft Fabric mod that changes how armor works to follow Payday 2's armor mechanics:
- **Armor absorbs ALL damage** when armor points remain (even 1 point absorbs everything)
- **Armor depletes** when absorbing damage (each hit reduces armor points)
- **Health takes damage** only when armor reaches 0
- **Armor regenerates** after 2.5 seconds of no damage, over 0.5 seconds (0% → 100%)
- **Armor swap** carries depletion percentage (no infinite tanking exploit)
- **Damage animations** — blink flash + low armor shake

Uses **vanilla Minecraft armor materials and values** (Leather, Iron, Gold, Diamond, Netherite, Chainmail).

## Core Mechanics

### Damage Flow
```
Player takes damage
    ↓
BYPASSES_ARMOR tag? → YES → Damage goes to health directly (void, /kill)
    ↓
    NO
    ↓
Has armor points? → YES → Armor absorbs ALL damage (even 1 point absorbs everything)
    ↓                    ↓
    NO              Armor depletes by damage amount, excess damage is GONE
    ↓                    ↓
Health takes damage   Armor = 0? → YES → Next hit damages health
                         ↓
                        NO → Player fine
```

### Armor Recovery
```
Last hit time tracked per player
    ↓
2.5 seconds elapsed since last hit? → YES → Start recovery from current armor
    ↓                                           ↓
NO                                        Recover current → max over 0.5 seconds
    ↓                                           ↓
Wait                                        Armor fully restored
```

### Armor Swap (Depletion Carryover)
```
Player swaps armor mid-combat
    ↓
Calculate depletion %: 1 - (currentArmor / maxArmor)
    ↓
New armor starts at: newMax * (1 - depletion%)
    ↓
If was fully depleted (100%)? → YES → Starts at 0% + forces 2.5s delay
```

### Damage Animation
```
Armor takes damage
    ↓
Blink effect: Icons flash (alternate visible/hidden) for 1 second
    ↓
Armor < 25% of max OR armor = 0? → YES → Shake effect: Random Y offset on icons
```

### Vanilla Armor Values (Total per set)
| Material | Helmet | Chestplate | Leggings | Boots | Total |
|----------|--------|------------|----------|-------|-------|
| Leather  | 1      | 3          | 2        | 1     | 7     |
| Chainmail| 2      | 5          | 4        | 1     | 12    |
| Iron     | 2      | 6          | 5        | 2     | 15    |
| Gold     | 2      | 6          | 5        | 2     | 15    |
| Diamond  | 3      | 8          | 6        | 3     | 20    |
| Netherite| 3      | 8          | 6        | 3     | 20    |

## Architecture

### Files

#### Server-Side (src/main/)

1. **`kotlin/com/example/armor/ArmorManager.kt`**
   - Kotlin object singleton
   - Track armor points per player (`currentArmor`, `maxArmor`)
   - Track last hit time + hit animation time
   - Handle armor recovery timer (2.5s delay, 0.5s recovery)
   - Depletion carryover on armor swap
   - Calculate total armor from equipped items via `player.getArmorValue()`
   - Sync state to client on every change

2. **`kotlin/com/example/armor/ArmorNetwork.kt`**
   - Custom packet payload for server→client armor sync
   - Sends `currentArmor`, `maxArmor`, `isRecovering`, `recoveryProgress`, `hitAnimationTime`
   - Uses Fabric Networking API (`PayloadTypeRegistry`)

3. **`java/com/example/mixin/LivingEntityArmorMixin.java`**
   - Target: `net.minecraft.world.entity.LivingEntity`
   - Inject into `actuallyHurt()` at HEAD to intercept damage
   - If armor absorbs all → cancel `actuallyHurt()` entirely (no health damage)
   - If armor depleted → let vanilla handle health damage
   - Skips if damage bypasses armor (`DamageTypeTags.BYPASSES_ARMOR`)

4. **`kotlin/com/example/TemplateMod.kt`**
   - Registers packet type (`PayloadTypeRegistry.clientboundPlay()`)
   - Registers `ServerTickEvents.END_SERVER_TICK` for armor recovery
   - Registers player join/leave events for state init/cleanup

#### Client-Side (src/client/)

5. **`kotlin/com/example/client/armor/ClientArmorState.kt`**
   - Receives armor state from server via network packets
   - Stores `currentArmor`, `maxArmor`, `isRecovering`, `recoveryProgress`, `hitAnimationTime`
   - Tracks `isBlinking` (1s flash after damage)
   - Tracks `isShaking` (when armor < 25%)
   - Ticks every frame to update animation state

6. **`java/com/example/client/mixin/ArmorBarMixin.java`**
   - Target: `net.minecraft.client.gui.Hud`
   - Inject into `extractArmor()` to replace vanilla armor bar
   - **Position**: Above health bar (vanilla position)
   - **Behavior**:
     - Hides completely when `maxArmor == 0` (no armor equipped)
     - Shows 100% when armor is full
     - Depletes on damage
     - Uses vanilla armor sprites (empty/half/full)
   - **Animations**:
     - Blink: Icons flash every 3 ticks for 1 second after damage
     - Shake: Random Y offset when armor < 25% or armor = 0

7. **`kotlin/com/example/client/TemplateModClient.kt`**
   - Registers client-side packet receiver for armor sync
   - Ticks `ClientArmorState` every frame for animations

### Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                        SERVER SIDE                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  LivingEntityArmorMixin (actuallyHurt @ HEAD)              │
│       │                                                     │
│       ▼                                                     │
│  ArmorManager.absorbDamage(player, damage)                  │
│       │                                                     │
│       ├── Detect armor swap → carry depletion %            │
│       ├── If armor > 0: armor -= damage                    │
│       │   └── Return 0 — excess damage is gone (DSOD)     │
│       ├── If armor = 0: let vanilla handle health damage   │
│       ├── Update lastHitTime + hitAnimationTime            │
│       └── Sync to client                                   │
│                                                             │
│  ArmorManager.tick() (runs every server tick)              │
│       │                                                     │
│       ├── Detect armor change → carry depletion %          │
│       ├── If no armor: clear state, sync to client         │
│       ├── If (currentTime - lastHitTime > 2.5s)            │
│       │       └── Start recovery: interpolate current→max  │
│       │           over 0.5s                                │
│       └── Sync to client on state change                   │
│                                                             │
│  ArmorNetwork.sendToPlayer(player)                         │
│       └── Send packet with all state + hitAnimationTime    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ (Fabric Networking API)
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT SIDE                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  TemplateModClient                                         │
│       │                                                     │
│       ▼                                                     │
│  ClientPlayNetworking.registerGlobalReceiver()             │
│       │                                                     │
│       ▼                                                     │
│  ClientArmorState.updateFromServer()                       │
│       │                                                     │
│       ▼                                                     │
│  ClientArmorState.tick() — update blink/shake state        │
│       │                                                     │
│       ▼                                                     │
│  ArmorBarMixin.extractArmor()                              │
│       │                                                     │
│       ▼                                                     │
│  Render custom armor bar:                                  │
│       - Hide if no armor (maxArmor == 0)                   │
│       - Empty sprites as background                        │
│       - Full/half sprites as foreground                    │
│       - Blink: flash every 3 ticks for 1s after damage    │
│       - Shake: random Y offset when armor < 25%           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Status

### Completed
- [x] Server-side armor absorption (replaces vanilla % reduction)
- [x] DSOD-style: 1 armor absorbs ALL damage, excess is gone
- [x] Armor state tracking per player
- [x] Recovery system (2.5s delay + 0.5s smooth fill)
- [x] Armor swap depletion carryover (prevents infinite tanking)
- [x] Server→client sync via Fabric Networking API
- [x] Custom armor bar HUD (above health)
- [x] Hide HUD when no armor equipped
- [x] Show 100% when armor is full, deplete on hit
- [x] Blink animation (flash for 1s after damage)
- [x] Shake animation (when armor < 25% or armor = 0)
- [x] Player join/leave state management

### Not Implemented (Future)
- [ ] Armor-piercing mechanic (PD2 sniper: reduce armor + leftover to health)
- [ ] Sound effects (equip, hit, recovery complete)
- [ ] Armor breaking animation/effect
- [ ] Config file for tuning values
- [ ] Multiplayer testing
- [ ] Armor stand interaction

## Technical Details

### Mixin Injection Points
- **`LivingEntity.actuallyHurt(ServerLevel, DamageSource, float)`**: Intercepts at HEAD, cancels entirely when armor absorbs all damage
- **`Hud.extractArmor(...)`**: Replaces vanilla armor bar rendering

### Data Synchronization
- **Fabric Networking API** with custom `CustomPacketPayload`
- Packet type: `minecraft:template-mod/armor_sync`
- Packet fields: `currentArmor` (float), `maxArmor` (float), `isRecovering` (boolean), `recoveryProgress` (float), `hitAnimationTime` (long)
- Sent on: damage absorbed, armor equip/unequip/swap, recovery tick, player join

### State Management
```kotlin
// Server-side
data class PlayerArmorState(
    var currentArmor: Float,      // Current armor points
    var maxArmor: Float,          // Max armor (from equipped items)
    var lastHitTime: Long,        // Timestamp of last damage (game ticks)
    var isRecovering: Boolean,    // Currently in recovery phase
    var recoveryProgress: Float,  // 0.0 to 1.0 during recovery
    var recoveryStartTime: Long,  // When recovery started
    var recoveryStartArmor: Float, // Armor value when recovery started
    var hitAnimationTime: Long    // When last damage occurred (for blink)
)

// Client-side
object ClientArmorState {
    var currentArmor: Float       // Received from server
    var maxArmor: Float           // Received from server
    var isRecovering: Boolean     // Received from server
    var recoveryProgress: Float   // Received from server
    var hitAnimationTime: Long    // Received from server
    var isBlinking: Boolean       // Flash effect (1s after damage)
    var isShaking: Boolean        // Shake effect (armor < 25%)
}
```

### Constants
```kotlin
RECOVERY_DELAY_TICKS = 50L      // 2.5 seconds at 20 TPS
RECOVERY_DURATION_TICKS = 10L    // 0.5 seconds at 20 TPS
```

## What This Mod Does NOT Change

- Vanilla armor values (Leather=7, Iron=15, etc.)
- Armor durability (items still break)
- **All enchantments stay vanilla** (Protection, Fire Protection, Blast Protection, Projectile Protection - no changes)
- Armor trimming
- Armor stands
- Mob armor

## Testing Checklist

- [ ] No armor equipped → no HUD shown
- [ ] Equip armor → HUD appears at 100%
- [ ] Zombie attack with full armor → armor depletes, health unchanged
- [ ] Zombie attack with 1 armor left → armor absorbs ALL damage, health unchanged
- [ ] Zombie attack with 0 armor → health takes damage
- [ ] Wait 2.5 seconds → armor recovers over 0.5 seconds
- [ ] Get hit during recovery → recovery resets, damage applies
- [ ] Remove all armor → HUD disappears
- [ ] Swap armor at 50% → new armor starts at 50%
- [ ] Swap armor at 0% → new armor starts at 0% + 2.5s delay
- [ ] Different armor materials have correct total values
- [ ] Blink animation plays after taking damage
- [ ] Shake animation plays when armor < 25%
- [ ] Shake animation plays when armor = 0
- [ ] Armor bar displays correctly
- [ ] Multiplayer sync works
- [ ] Armor breaks at 0 durability normally
