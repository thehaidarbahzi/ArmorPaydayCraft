# Product Requirements Document: Payday 2-Style Armor System

## Overview

A Minecraft Fabric mod that reworks the armor system to follow Payday 2's armor mechanics. Armor absorbs **all** incoming damage until depleted, then health takes damage. After 2.5 seconds without hits, armor smoothly recovers to full over 0.5 seconds.

## Project Details

- **Mod ID**: `template-mod`
- **Minecraft Version**: 26.2
- **Mod Loader**: Fabric (Loader 0.19.3)
- **Language**: Kotlin 2.4.10 + Java 25 (mixins)
- **Build System**: Gradle with Fabric Loom 1.17-SNAPSHOT

## Requirements

### Functional

1. **Armor Absorption**: When a player has armor, ALL damage is absorbed by armor (not percentage reduction like vanilla). Even 1 armor point absorbs all incoming damage. Excess damage beyond armor is completely gone (DSOD-style).
2. **Health Damage**: When armor reaches 0, subsequent damage hits health directly.
3. **Armor Recovery**: After 2.5 seconds of no damage, armor recovers from current value to max over 0.5 seconds with smooth interpolation. (e.g., 15/20 → recovers 15→20, not 0→20)
4. **Hit During Recovery**: If the player is hit while recovering, recovery resets and damage applies immediately.
5. **Armor Swap Depletion**: When swapping armor mid-combat, depletion percentage carries over. If armor was at 50%, new armor starts at 50%. If fully depleted, new armor starts at 0% with 2.5s recovery delay.
6. **Armor HUD**: Custom armor bar above health that:
   - Hides completely when no armor is equipped
   - Shows 100% when armor is full
   - Depletes as armor absorbs damage
   - Fills smoothly during recovery
   - Blinks (flashes) for 1 second after taking damage
   - Shakes when armor is low (<25%) or depleted (0)
7. **Multiplayer**: Server-authoritative armor state synced to clients via network packets.

### Non-Functional

1. **Vanilla Armor Values**: Uses Minecraft's default armor values (Leather=7, Iron=15, Diamond=20, etc.)
2. **No Enchantment Changes**: All enchantments (Protection, Fire Protection, etc.) stay vanilla.
3. **No Durability Changes**: Armor items still break at 0 durability.
4. **Performance**: Minimal overhead — one tick handler per player, lightweight packets.

### Future (Not Implemented)

1. **Armor-Piercing Mechanic**: PD2-style sniper behavior — reduces armor first, leftover damage goes to health.

## Architecture

### Files

| File | Side | Purpose |
|------|------|---------|
| `ArmorManager.kt` | Server | Armor state tracking, damage absorption, recovery logic, depletion carryover |
| `ArmorNetwork.kt` | Server | Network packet definition and sending |
| `LivingEntityArmorMixin.java` | Server | Intercepts `actuallyHurt()` to cancel damage when armor absorbs all |
| `TemplateMod.kt` | Server | Entrypoint — registers packets, tick handler, player events |
| `ClientArmorState.kt` | Client | Stores received armor state, tracks blink/shake animation state |
| `ArmorBarMixin.java` | Client | Replaces vanilla armor bar rendering with custom HUD + animations |
| `TemplateModClient.kt` | Client | Entrypoint — registers packet receiver, ticks animation state |

### Data Flow

```
Server: Player takes damage
  → LivingEntityArmorMixin intercepts actuallyHurt()
  → ArmorManager.absorbDamage() reduces armor points
  → If armor absorbs all → cancel actuallyHurt() entirely
  → If armor depleted → let vanilla handle health damage
  → ArmorNetwork.sendToPlayer() syncs state to client

Server: Every tick
  → ArmorManager.tick() checks recovery timer + armor changes
  → After 2.5s: interpolate armor 0→100% over 0.5s
  → On armor swap: carry depletion percentage
  → Sync to client on state change

Client: Receives packet
  → ClientArmorState updated (including hitAnimationTime)
  → ClientArmorState.tick() updates blink/shake state
  → ArmorBarMixin reads state and renders HUD with animations
```

## Configuration

| Constant | Value | Description |
|----------|-------|-------------|
| `RECOVERY_DELAY_TICKS` | 50 | 2.5 seconds at 20 TPS |
| `RECOVERY_DURATION_TICKS` | 10 | 0.5 seconds at 20 TPS |

## Network Protocol

- **Channel**: `minecraft:template-mod/armor_sync`
- **Direction**: Server → Client
- **Fields**: `currentArmor` (float), `maxArmor` (float), `isRecovering` (boolean), `recoveryProgress` (float), `hitAnimationTime` (long)
- **Triggers**: Damage absorbed, armor equip/unequip/swap, recovery tick, player join

## What This Mod Does NOT Change

- Vanilla armor values
- Armor durability
- Enchantments
- Armor trimming
- Armor stands
- Mob armor

## License

CC0-1.0 (Public Domain)
