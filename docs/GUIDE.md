# Zian RCT 0.2.4 — English guide

**Language notice:** The current in-game interface, preset dialogues and administrator messages are primarily Spanish. English localization is incomplete. The English guide explains the controls and includes their Spanish button names. Changing Minecraft to English does not translate administrator-written NPC names, dialogues or configuration text.

## Required API, not RCTMod

Install **RCT API separately; do not install RCTMod alongside Zian RCT**. Requirements: Minecraft 1.21.1, NeoForge, Java 21, Cobblemon 1.8.4, RCT API 0.16.2-beta, Architectury 13.x and Kotlin for Forge 5.12.x within the declared ranges. AVECOINS is optional for configured currency rewards. Zian supplies its own trainer implementation using the external API; it does not bundle every RCTMod spawner/catalog feature.

## Players: battle and progress

Interact with a configured trainer. Read dialogue/prerequisites/party-cap feedback. For an invitation choose **Aceptar** (Accept) or **Rechazar** (Reject). Invitations belong to that player/trainer, expire and require you to remain nearby.

Normal/legendary battle participation needs **no LuckPerms grant, OP, zianrct.battle or zianrct.legendary.claim** in this version. Progression, team eligibility, cooldowns, distance and completed-trial checks still apply. Public participation never grants trainer editing. Win the eligible battle to obtain its configured medal/reward. `/medallas` shows earned/next medals. A party above its cap must be adjusted; OP does not bypass native progression or downgrade existing Pokémon.

## Administrators: create/edit trainers

1. Open `/zianrct npc` with administrator access; create or select a definition. Selection is not saving.
2. Set visible name, appearance and home/position. Named skins select classic/slim arms for that texture; existing appearance identities are retained.
3. Configure **1–6 Pokémon**, levels and difficulty. Use the species selector and automatic moves, or select legal manual moves. A selection changes the draft, which must be saved.
4. Set single/double format, dialogue, repeat limits/cooldowns and rewards; save explicitly. Test as a normal player with the intended progress. Independent challenges need not use the league sequence unless configured.
5. Medal/cap/reward editing remains permission-protected. Do not grant administrative permissions to make public battles work.

## Repeat rewards and legendary trials

Unique/repeat rewards use recorded owner, trainer and battle identity. Read NPC availability to distinguish a cooldown/completed reward from permission failure. A custodian trial can require an earlier boss victory and uses its configured single Pokémon.

A qualifying real victory reserves the legendary result before PC delivery: UUID, species, IVs and shiny result persist through recovery. A full PC leaves a pending result, not a free reroll. Completed one-time trials stay completed after restart. `/zianrct legendary status` inspects your state. Public recovery/claim cannot manufacture a victory. An administrator's confirm-delivered action records verified evidence and must not guess whether delivery happened.

## Compatibility

Read [standalone migration and licenses](RCTAPI-STANDALONE.md) before removing RCTMod from an existing world; test a complete copy first because that mod's own blocks/items may disappear. Supported legacy player progress is not reimported over newer progress on every restart.

Legendary Monuments 8.2 callbacks may assume another trainer implementation; a server-local callback guard is **not bundled** in this mod. That combination needs its corresponding fix. NPC protocol: **npc9**.

## Installation, updating and recovery

Use Minecraft **1.21.1**, **Java 21** and **NeoForge**, with the separate required libraries listed in the README. Stop the server and back up the entire world, configuration and mod list before replacing a version. Install the same current JAR on the server and every client. Remove the previous JAR, keep the required libraries on both sides, start and inspect the log, then verify your existing configuration and progress. For rollback, stop and restore the complete matching backup and mod versions together. Do not edit schema numbers to force migration.

Youer has been tested; this does not guarantee every hybrid server/plugin combination. Command-whitelist plugins may separately block public commands. Allow the needed public roots while keeping administrative operations restricted. LuckPerms is optional; administrator configuration remains protected.

Keep world data and configuration together. Optional SQLite maintenance is explicit, not required automatically by an update. Never edit a live database or delete a journal to clear an error. For an uncertain payment/delivery, keep its reference, reconcile the recorded outcome with inventory/PC/wallet state and use the administrator review flow. Confirmation records a verified result; it must never be used to guess or generate a second reward.

Report the mod/library versions, reproduction steps and sanitized logs through this repository's Issues. Do not upload credentials, tokens, player databases or private world backups.
