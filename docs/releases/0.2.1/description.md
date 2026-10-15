# Zian RCT

Create custom trainer NPCs, teams, progression chains, level caps, medals and rewards through native administrative menus.

Supported settings include single/double battles, repeat conditions, cooldowns and unique or timed rewards. Legendary trials can require a qualifying boss victory and reserve a specific result before delivery. Durable receipts and administrative recovery protect unfinished operations.

The responsive medal case shows earned and locked medals with progression details. Trainer dialogue uses a consistent visual style.

Install matching client/server versions with Minecraft 1.21.1, Java 21, NeoForge 21.1.x, Cobblemon 1.8.2, RCT API 0.16.2-beta, Architectury API 13.x and Kotlin for Forge 5.12.x. RCT API is a separate required dependency. Do not install RCTMod alongside this standalone trainer implementation.

AVECOINS 2.3/2.4/2.5 is optional for currency rewards. LuckPerms is optional for permission management, including compatible Youer/Bukkit environments.

Compatibility note: Legendary Monuments 8.2 victory callbacks can fail when they assume native NPC entities. Our test server uses a local callback guard, which is not included in this mod. That combination needs the corresponding fix.

Back up the world before upgrades or migration. Uncertain deliveries require administrative review instead of an automatic retry.

## Español

Cree entrenadores, equipos, progresión, level cap y medallas. Incluye combates dobles, recompensas únicas o por tiempo y pruebas legendarias. Requiere RCT API 0.16.2-beta por separado; no instale RCTMod junto a esta implementación.

## AVECOINS compatibility update

Version 0.2.1 accepts the inspected AVECOINS 2.5 wallet. Later version numbers are accepted only when the three reviewed wallet classes have byte-identical fingerprints and pass reflection checks. Real contract changes require review; no arbitrary future update is automatically trusted. Existing wallets and durable receipts are preserved.
