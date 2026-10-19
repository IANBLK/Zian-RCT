# Zian RCT 0.2.4

**Language notice:** The current in-game interface, preset dialogues and administrator messages are primarily Spanish. English localization is incomplete. The English guide explains the controls and includes their Spanish button names. Changing Minecraft to English does not translate administrator-written NPC names, dialogues or configuration text.

[English guide](docs/GUIDE.md) · [Guía en español](docs/GUIA_ES.md) · [Updating and recovery](docs/UPDATING.md) · [Current changes](CHANGELOG.md) · [Download](https://github.com/IANBLK/Zian-RCT/releases/latest)

**Requires RCT API, not RCTMod.** Do not install RCTMod alongside Zian RCT.

**Requiere RCT API; no necesita RCTMod.** No instales RCTMod junto con Zian RCT.

Create custom trainer NPCs, teams, progression chains, level caps, medals and rewards through native administrative menus.

Supported settings include single/double battles, repeat conditions, cooldowns and unique or timed rewards. Legendary trials can require a qualifying boss victory and reserve a specific result before delivery. Durable receipts and administrative recovery protect unfinished operations.

The responsive medal case shows earned and locked medals with progression details. Trainer dialogue uses a consistent visual style.

Install matching client/server versions with Minecraft 1.21.1, Java 21, NeoForge 21.1.x, Cobblemon 1.8.2, RCT API 0.16.2-beta, Architectury API 13.x and Kotlin for Forge 5.12.x. RCT API is a separate required dependency. Do not install RCTMod alongside this standalone trainer implementation.

AVECOINS 2.3/2.4/2.5 is optional for currency rewards. LuckPerms is optional for permission management, including compatible Youer/Bukkit environments.

Compatibility note: Legendary Monuments 8.2 victory callbacks can fail when they assume native NPC entities. Our test server uses a local callback guard, which is not included in this mod. That combination needs the corresponding fix.

Back up the world before upgrades or migration. Uncertain deliveries require administrative review instead of an automatic retry.

## Español

Cree entrenadores, equipos, progresión, level cap y medallas. Incluye combates dobles, recompensas únicas o por tiempo y pruebas legendarias. Requiere RCT API 0.16.2-beta por separado; no instale RCTMod junto a esta implementación.

## Public battles
Battle invitations are public player actions and do not require LuckPerms. Progression and safety checks still apply; trainer configuration is restricted to administrators.

## Skin credits
The maintainer identifies https://es.namemc.com/ as the source of the 27 additional skins and states they are publicly distributed. Original artwork rights remain with their respective creators; Zian does not relicense these skins as MIT.


## Latest update — 0.2.4

Players can interact with league and independent trainer challenges, including legendary trials, and accept/reject their own battle invitations without permission grants or a LuckPerms lookup. Progression, level caps, invitation owner/trainer matching, expiry, distance and native battle eligibility remain enforced. Administrative commands remain protected. Includes the recent compact medal case, next-medal guidance, owner-bound reward references, searchable completion and 27 additional named NPC skins with classic/slim arm models (33 total). Existing appearance indices and league identities are preserved. Client/server NPC protocol npc9.

## Novedades — Español

Los jugadores pueden iniciar desafíos y aceptar/rechazar sus invitaciones sin permisos de LuckPerms. Se mantienen progresión, level cap, distancia, caducidad y titularidad de la invitación. La administración sigue protegida. Incluye medallero compacto, indicación de siguiente medalla, referencias personales de premios y 27 skins adicionales con brazos clásicos/delgados.

Minecraft 1.21.1 · NeoForge · Java 21. Install the same mod version on client and server. Back up your world before upgrading; replace the old JAR instead of keeping both copies.
