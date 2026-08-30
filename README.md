<img src=".github/assets/icon.png" alt="Mod Icon" width="128" />

# ArmorPaydayCraft

A Minecraft mod that replaces vanilla armor with Payday 2's armor system. Armor works as a complete damage shield, not a percentage reduction.

## How It Works

In vanilla Minecraft, armor reduces damage by a percentage. With full diamond armor, a zombie hit still hurts you a little. This mod changes that completely.

**If you have armor, you take zero health damage. Period.**

Even 1 armor point absorbs an incoming hit for 255 damage. The armor depletes, but that excess damage is gone. It never reaches your health bar. When armor hits 0, the next hit goes straight to your health.

## Features

- **Full absorption**: any amount of armor blocks all damage from reaching health
- **Armor depletion**: each hit reduces your armor points by the damage amount
- **Recovery**: armor regenerates after 2.5 seconds of not getting hit, filling back up over 0.5 seconds
- **Armor swap carryover**: swap armor mid-combat and the depletion percentage carries over. No free refills
- **Damage blink**: armor bar icons flash for 1 second after taking a hit
- **Low armor shake**: armor bar shakes when armor is below 25% or fully depleted
- **Clean HUD**: armor bar hides completely when no armor is equipped
- **Multiplayer ready**: server-authoritative state, synced to all clients

## Vanilla Armor Values

The mod uses vanilla armor values. No changes to what each material provides:

| Material  | Total Armor |
| --------- | ----------- |
| Leather   | 7           |
| Chainmail | 12          |
| Iron      | 15          |
| Gold      | 15          |
| Diamond   | 20          |
| Netherite | 20          |

## What Stays Vanilla

- Armor durability (items still break)
- All enchantments (Protection, Fire Protection, Blast Protection, Projectile Protection)
- Armor trimming
- Armor stands
- Mob armor

## Configuration

| Setting           | Default | Description                                        |
| ----------------- | ------- | -------------------------------------------------- |
| Recovery delay    | 2.5s    | Time after last hit before armor starts recovering |
| Recovery duration | 0.5s    | Time to fill from current value to full            |

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3+
- Fabric API

## Installation

1. Install Fabric Loader
2. Download this mod and Fabric API
3. Drop both into your `mods` folder
4. Launch Minecraft

## License

CC0-1.0 (Public Domain)
