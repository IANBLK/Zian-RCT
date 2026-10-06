**🏅 ZIAN RCT — BUILD YOUR COBBLEMON CHALLENGES**

Create a trainer league, place your own challengers, and reward players for their victories. Zian RCT expands **Radical Cobblemon Trainers** with a medal interface, an in-game NPC manager, custom trainer creation, configurable loot, and unique legendary trials.

Built for **Minecraft 1.21.1 and NeoForge**, with gameplay testing on **Youer 1.21.1**.

**🏆 MEDALS AND PROGRESSION**

Collect the ten medals included in the **Rassvet profile** by defeating their associated trainers. View earned and locked medals in a compact interface with custom artwork and pagination.

Open the medal case with **M** or **/medals**. The key can be changed in Minecraft's Controls settings.

Administrators can manage medals, inspect player progress, and adjust progression through protected commands. Independent trainers use their own IDs and reward settings; existing RCT level caps remain in effect.

**🛠️ MANAGE NPCs INSIDE THE GAME**

Open **/zianrct npc** to browse trainers currently loaded in your dimension. The manager shows their coordinates and distance so you can identify the NPC you want to edit.

Create NPCs from available trainer definitions, modify existing ones, or delete a selected NPC with confirmation. Move an NPC to your current block and facing direction, configure persistence, and allow or block its movement.

NPC placement and saved settings survive server restarts.

**🎯 CREATE YOUR OWN TRAINERS**

Define independent trainers from scratch through the in-game editor. Customize their visible name, difficulty, team, levels, included skin, and dialogue.

Choose **Easy, Normal, Hard, or Boss**, build a team of **1–6 Pokémon**, and select **Single or Double battles**. Double battles require at least two Pokémon.

Searchable Pokémon and move selectors help build your team. Choose up to four moves per Pokémon, or use **Automatic Moves**: select the species, level, and difficulty, then let the server prepare the moves when you save.

Easy and Normal use available level-up moves. Hard and Boss also consider TM, tutor, egg, and evolution moves, prioritizing attack strength, accuracy, same-type bonuses, coverage, and support. Manual editing remains available.

Difficulty also applies automatic IV, EV, AI, and held-item presets. Hard and Boss opponents use perfect IVs. Their battle equipment is configured separately from the loot players receive.

**🎨 APPEARANCE AND DIALOGUE**

Choose from **six bundled trainer skins** and configure dialogue for battle start, player victory, and player defeat.

Interaction replies explain cooldowns, busy battles, missing Pokémon, or a team above the current level cap. Independent trainers report their own battle requirements.

The current administrative interface uses Spanish labels. Importing additional PNG skins through the editor is not included in this beta.

**🎁 CONFIGURABLE TRAINER LOOT**

Reward victories with Minecraft or modded items, including Cobblemon items, and optionally with **AVECOINS currency**.

Copy an item stack held in your main hand into the reward configuration, preserving its components. Choose a **one-time reward** or a **repeatable reward with a per-player waiting period**.

Repeatable reward messages show the trainer's readable name and when the next reward becomes available. Use **Shift + right-click** on a configured reward trainer to check your availability.

Pending rewards remain recorded when delivery cannot complete. Claim IDs support autocomplete and clickable claim actions in chat. Uncertain deliveries are held for administrative review instead of being automatically replayed.

**🐉 UNIQUE LEGENDARY TRIALS**

Link two independent trainers to create a special challenge:

- Defeat a **Boss** trainer to receive its configured loot and unlock a second NPC for that player.
- Speak to the legendary custodian to begin its **single-Pokémon, Single battle**.
- Win to receive a new Pokémon of the configured legendary or mythical species and level, delivered directly to your **PC**.
- The reward has six random IVs between **25 and 30** and a configurable shiny chance, **1 in 512 by default**.
- Losing allows another attempt. Confirmed receipt permanently closes that trial for the player, including after a restart.

Configure the link in the custodian's **Trial / Prueba** tab. The unlock is recorded per player; granting a general command permission does not qualify someone for the encounter.

Reward UUID, IVs, and shiny result are fixed before delivery. Existing trial reward settings are locked once player progress exists. The automatic delivery is verified against Cobblemon's native file-backed PC/party storage; unsupported storage providers leave the reward pending.

**📜 USEFUL COMMANDS**

**/medals** — Open your medal case.

**/zianrct npc** — Open the administrator NPC manager.

**/zianrct reward pending** — View your pending trainer rewards.

**/zianrct reward claim <operationId>** — Claim an eligible pending reward; use Tab to autocomplete its ID.

**/zianrct reward next <trainerId>** — Check your reward availability.

**/zianrct legendary status** — Check your legendary trial progress.

**/zianrct legendary claim** — Claim a pending legendary reward or verify an interrupted delivery.

**/zianrct reload** — Reload Zian RCT configuration.

**🔐 LUCKPERMS SUPPORT**

Manage player and administrator access through **LuckPerms groups**, using the NeoForge mod or the Bukkit plugin on Youer.

Player permissions include **zianrct.medals**, **zianrct.battle**, **zianrct.rewards.claim**, and **zianrct.legendary.claim**. Editor and recovery permissions are separate administrative nodes.

Explicit denials also apply to operators. Without LuckPerms, administrative actions use the level-2 OP fallback. OP status does not bypass completed legendary trials or native team level caps.

**📦 INSTALLATION**

Requires:

- **Minecraft 1.21.1**
- **Java 21**
- **NeoForge 21.1.x**
- **Cobblemon 1.8.1**
- **Radical Cobblemon Trainers 0.19.2-beta**
- **Radical Cobblemon Trainers API 0.16.1-beta**
- **Architectury 13.0.11**
- **Kotlin for Forge 5.12.0**

Install **the same Zian RCT version on the client and server**, together with the required dependencies.

**AVECOINS 2.3 or 2.4** is optional for currency rewards. **LuckPerms** is optional for permission management. **Zian GUI** can provide a shortcut to the medal interface.

**🚧 BETA — ACTIVE DEVELOPMENT**

This release includes custom trainer creation, persistent NPC management, repeated loot, and unique legendary rewards. Saving trainer definitions reloads server data and may briefly pause a heavily modded server.

When reporting an issue, include your mod versions, server platform, relevant logs, and the steps needed to reproduce it.

**Created by IANBLK • Minecraft 1.21.1 • NeoForge • Tested on Youer**
