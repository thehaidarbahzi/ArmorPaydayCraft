# Product Requirements Document: Payday 2-Style Armor System

## Overview

A Minecraft Fabric mod that reworks the armor system to follow Payday 2's armor mechanics. Armor absorbs **all** incoming damage until depleted, then health takes damage. After 2.5 seconds without hits, armor smoothly recovers to full over 0.5 seconds. Includes a directional damage indicator around the crosshair.

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
3. **Armor Recovery**: After 2.5 seconds of no damage, armor recovers from current value to max over 0.5 seconds with smooth interpolation.
4. **Hit During Recovery**: If the player is hit while recovering, recovery resets and damage applies immediately.
5. **Armor Swap Depletion**: When swapping armor mid-combat, depletion percentage carries over.
6. **Armor HUD**: Custom armor bar above health that hides when no armor, depletes on hit, blinks for 1s, shakes when low.
7. **Damage Indicator**: Directional indicator orbits the crosshair showing where damage came from.
   - White indicator for armor damage (armor absorbed)
   - Red indicator for health damage (armor depleted)
   - Fades out over 3 seconds
   - Scales with GUI size
8. **Multiplayer**: Server-authoritative armor state synced to clients via network packets.

### Non-Functional

1. **Vanilla Armor Values**: Uses Minecraft's default armor values
2. **No Enchantment Changes**: All enchantments stay vanilla
3. **No Durability Changes**: Armor items still break at 0 durability
4. **Performance**: Minimal overhead

## Architecture

### Files

| File | Side | Purpose |
|------|------|---------|
| `ArmorManager.kt` | Server | Armor state tracking, damage absorption, recovery logic, depletion carryover |
| `ArmorNetwork.kt` | Server | Network packet definition and sending |
| `LivingEntityArmorMixin.java` | Server | Intercepts `getDamageAfterArmorAbsorb` to cancel damage when armor absorbs all |
| `TemplateMod.kt` | Server | Entrypoint — registers packets, tick handler, player events |
| `ClientArmorState.kt` | Client | Stores received armor state, tracks blink/shake animation state |
| `DamageIndicatorState.kt` | Client | Tracks active damage indicators (direction, lifetime, damage type) |
| `DamageIndicatorHud.kt` | Client | Renders directional damage indicator with rotation and alpha fade |
| `ArmorBarMixin.java` | Client | Replaces vanilla armor bar rendering with custom HUD + animations |
| `TemplateModClient.kt` | Client | Entrypoint — registers packet receiver, ticks animation state, registers HUD element |

### Network Protocol

- **Channel**: `minecraft:template-mod/armor_sync`
- **Direction**: Server → Client
- **Fields**: `currentArmor` (float), `maxArmor` (float), `isRecovering` (boolean), `recoveryProgress` (float), `hitAnimationTime` (long), `isHealthDamage` (boolean)
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
