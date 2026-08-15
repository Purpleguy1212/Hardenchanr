# HardEnchant

A Paper/Spigot plugin that lets admins push vanilla enchantments above their
normal max level (e.g. Sharpness VI–X), while making sure those levels can
**never** be obtained through normal gameplay — not from loot chests, fishing,
mob drops, villager trades, the enchanting table, or the anvil.

## How it works

- **Command-only source.** The only way to get an above-vanilla enchant level
  is `/hardenchant give` or `/hardenchant book`, both of which require the
  `hardenchant.admin` permission (default: op).
- **Everything else gets clamped.** A listener watches loot generation
  (chests, fishing, mob loot tables), direct entity drops, villager trade
  offers, and enchanting-table results, and clamps any enchant level back
  down to that enchant's real vanilla max the instant it's generated.
- **Anvil is locked down.** If either item involved in an anvil combine
  already carries an above-vanilla enchant, the combine is blocked outright
  (repairing with plain materials and renaming still work fine) — so players
  can't use the anvil to merge their way past the cap either.

Which enchants can be extended, and how high, is configurable in
`config.yml` under `extended-enchants`. Anything not listed there stays at
its normal vanilla max even via commands.

## Building

Requires Java 17+ and Maven, and internet access to PaperMC's Maven repo.

```
mvn package
```

The output jar will be at `target/HardEnchant.jar`. Drop it in your server's
`plugins/` folder and restart.

**Important:** open `pom.xml` and set the `paper-api` version to match your
server's Minecraft version (e.g. `1.20.4-R0.1-SNAPSHOT`, `1.21.1-R0.1-SNAPSHOT`).
You can find the right string at
https://repo.papermc.io/#browse/browse:maven-public:io%2Fpapermc%2Fpaper%2Fpaper-api

## Commands

All require `hardenchant.admin`.

| Command | Effect |
|---|---|
| `/hardenchant give <player> <enchant> <level>` | Apply an enchant (any level, clamped to the configured admin max) to the item in the player's main hand. |
| `/hardenchant book <player> <enchant> <level>` | Give the player an enchanted book with that enchant/level. |
| `/hardenchant remove <player> <enchant>` | Strip an enchant from the item in the player's main hand. |
| `/hardenchant list` | Show all configured extended enchants and their vanilla vs. admin max levels. |
| `/hardenchant reload` | Reload `config.yml`. |

Enchant names use their vanilla id, e.g. `sharpness`, `efficiency`,
`protection`, `unbreaking`, `fortune`, `looting`.

## Example

```
/hardenchant give Notch sharpness 8
/hardenchant book Notch efficiency 9
```

## Notes on mechanics

Applying a level above vanilla max via the API (`ItemMeta#addEnchant` with
`ignoreLevelRestriction = true`) is functional in vanilla combat/tool/armor
formulas — the game just scales the effect by level, it doesn't hard-cap at
apply time. The cap only exists in the generation code for loot tables, the
enchanting table, and villager trades, and in the anvil's combine logic —
which is exactly what this plugin's listener intercepts.
