# NeoMorph

The ultimate morph plugin for PaperMC — become any mob with full abilities, scaling, and stats.

## Features

- **60+ morphs** across passive, neutral, hostile, and boss categories
- **Unique abilities** for each mob — creeper explosions, enderman teleportation, warden sonic boom, dragon breath, and more
- **Accurate scaling** — your hitbox and camera height match the mob
- **Mob stats** — health, speed, attack damage, knockback resistance all change to match
- **Passive effects** — fire immunity for blazes, water breathing for fish, night vision for spiders
- **Player morphs** — disguise as any online player with their skin
- **GUI selector** — categorized inventory menu with pagination
- **Self-visibility** — see your own morph model with no collision (configurable)

## Requirements

- **PaperMC 1.21.1+** (not compatible with Spigot or Bukkit)
- **Java 21+**

No other plugins required — the disguise engine is built in.

## Installation

1. Download `NeoMorph-1.0.0.jar` from releases
2. Drop it into your server's `plugins/` folder
3. Restart your server
4. **Remove iDisguise** if you had it installed previously — NeoMorph includes its own embedded disguise engine

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/morph` | Open the morph selection GUI | `neomorph.use` |
| `/morph <mob>` | Morph into a specific mob | `neomorph.use` |
| `/morph <player>` | Morph into a player | `neomorph.player` |
| `/unmorph` | Return to normal form | `neomorph.use` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `neomorph.use` | Use the morph system | Everyone |
| `neomorph.boss` | Morph into boss mobs (Ender Dragon, Wither) | OP |
| `neomorph.player` | Morph into other players | OP |
| `neomorph.bypass.cooldown` | Bypass morph cooldown | OP |

## Abilities

Morph as a mob and **sneak** to activate its ability:

| Mob | Ability |
|-----|---------|
| Creeper | Explode (destroys blocks!) |
| Enderman | Teleport where you're looking |
| Ghast | Shoot fireballs |
| Blaze | Rapid fire charges |
| Wither | Launch wither skulls |
| Ender Dragon | Dragon breath cloud |
| Warden | Sonic boom |
| Iron Golem | Ground slam AoE |
| Phantom | Dive bomb |
| Evoker | Summon fangs |
| And 20+ more... | |

## Building from Source

```bash
mvn clean package
```

The compiled JAR will be at `target/NeoMorph-1.0.0.jar`.

## Configuration

The `config.yml` lets you customize explosion power, ability cooldowns, teleport range, and more. All settings have sensible defaults.

Key settings:
- `morph-self-visible: true` — Set to `false` if players experience collision/pushing with their morph on your specific server. Default `true`.

## Credits

NeoMorph includes an embedded disguise engine based on [**iDisguise**](https://github.com/LuisaGretworthy/iDisguise) by **Luisa R. Grether**. The original iDisguise source code has been refactored from a standalone plugin into an embedded engine, but the core disguise logic remains her work. See [CREDITS.md](CREDITS.md) for full attribution.

## License

NeoMorph's own code is released under the MIT License — see [LICENSE](LICENSE).

The embedded iDisguise engine (the `de.luisagrether.*` packages) is subject to the [iDisguise license](LICENSE-iDisguise.md), which requires **non-commercial use** and **proper attribution**. Please review it before distributing.
