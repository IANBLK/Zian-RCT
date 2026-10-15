**🏅 ZIAN RCT — YOUR LEAGUE, YOUR TRAINERS, YOUR CHALLENGES**

Build a Cobblemon league, create your own opponents, and reward players for overcoming your challenges. **Zian RCT** combines persistent NPCs, an in-game trainer editor, level progression, collectible medals, configurable loot, and unique legendary trials.

**Powered by RCTAPI. This 0.2 generation runs without RCTMod.** Install it on both client and server for **Minecraft 1.21.1 / NeoForge**. Gameplay has also been tested on **Youer 1.21.1**, including with a non-OP account.

**🏆 THE RASSVET LEAGUE AND MEDALS**

The included Rassvet profile contains ten trainer challenges: **Novato, Ferrum, Aquila, Voltar, Engranaje, Bruma, Cognitus, Forjax, Glacius, and Maestra Aurelia**.

Defeat the required predecessor to advance through the league, unlock higher levels, and earn the corresponding medal. The default progression starts at **level 10** and advances through **20, 30, 40, 50, 60, 70, 80, 90, and 100**. Glacius unlocks level 100; Aurelia remains the final challenge and medal.

Open the compact medal case with **M** or **/medals**. Its custom artwork distinguishes earned and locked medals. Progress, medals and completed victories remain saved after restarting.

**📈 LEVEL CAPS THAT APPLY FROM THE START**

Players must respect their unlocked level before defeating the first trainer. **XL candies, Rare Candies and experience awards through Cobblemon cannot raise a Pokémon above the current cap.**

Training resumes when the next level cap is unlocked. A candy rejected because the Pokémon is already at its cap is not consumed. OP status does not bypass this experience restriction.

Pokémon already above the cap are not automatically downgraded. League NPCs reject parties exceeding the unlocked cap. Independent custom challenges do not require completing the Rassvet trainer chain and do not advance it.

**🛠️ AN IN-GAME NPC MANAGER**

Use **/zianrct npc** to manage trainer NPCs loaded in your current dimension. The list shows their location and distance so you can identify the correct NPC.

- **Create** an NPC from a league or custom trainer definition.
- **Modify** its settings through the in-game interface.
- **Move** it to your current block and facing direction.
- Enable **persistence** and allow or block movement.
- **Delete** a selected NPC with confirmation.

Persistent NPCs keep their placement and settings after restarting. Deleting an NPC does not erase player medals or reward records.

**🎯 CREATE CUSTOM TRAINERS FROM SCRATCH**

Design independent opponents for money challenges, special events or legendary trials. Customize their **name, difficulty, team, Pokémon levels, battle format, skin and dialogue**.

Choose **Easy, Normal, Hard or Boss**, with **1–6 Pokémon** at levels **1–100**. Select **Single** or **Double** battles; Double battles require at least two Pokémon capable of battling on each side.

Searchable species and move selectors help you assemble teams. Choose up to four moves per Pokémon manually, or enable **Automatic Moves** and let the server prepare them from species, level and difficulty when you save.

Easy and Normal use suitable level-up moves. Hard and Boss also consider TM, tutor, egg and evolution moves, using attack strength, accuracy, same-type bonuses, coverage and support in their selection.

**⚔️ AUTOMATIC DIFFICULTY PRESETS**

Difficulty controls AI settings, IVs, EVs and battle equipment:

- **Easy:** 10 IVs in each stat, no EV investment and no held item.
- **Normal:** 20 IVs in each stat, no EV investment and an Oran Berry.
- **Hard:** perfect 31 IVs, 64 EVs per stat and a Sitrus Berry.
- **Boss:** perfect 31 IVs, 84 EVs per stat and Leftovers.

Battle equipment is separate from the loot awarded to players. Selecting a Pokémon and level is enough to build an automatic team; manual moves remain available.

**🎨 LEADER SKINS AND DIALOGUE**

The ten Rassvet leaders and master have **their own included skins**, assigned by trainer ID. Custom trainers can choose from **six additional bundled skin presets**.

Configure dialogue for battle start, player victory and player defeat. Replies appear in chat and can be displayed above the NPC, with messages explaining busy battles, resting time, missing Pokémon, completed encounters and level restrictions.

The administrative interface currently uses Spanish labels. Runtime PNG importing through the editor is not included in this release.

**🎁 ONE-TIME AND REPEATABLE REWARDS**

Configure Minecraft or modded items, including Cobblemon items, and optionally award **AVECOINS currency**. Add the item stack held in your main hand to preserve its quantity and components.

Choose a **one-time reward** or a **repeatable reward with a per-player waiting period**. Trainer messages use readable names and explain when the next reward becomes available. **Shift + right-click** on a configured reward trainer to check your eligibility.

Completed rewards and remaining waiting times survive server restarts. Pending deliveries remain recorded, with autocomplete and clickable claim actions. Uncertain deliveries are held for administrative review rather than automatically repeated.

**🐉 UNIQUE LEGENDARY TRIALS**

Link a **Boss** trainer to a second, legendary custodian through the **Trial / Prueba** tab:

1. Defeat the Boss to earn its configured reward and unlock the custodian for your account.
2. Interact with the custodian to challenge its configured legendary or mythical Pokémon in a **Single battle**.
3. Losing allows another attempt. Winning awards a new Pokémon of the configured species and level directly to your **PC**.
4. The reward has random IVs from **25 to 30 in all six stats** and a configurable shiny chance, **1 in 512 by default**.
5. Confirmed delivery closes that trial permanently for the player, including after restarting.

Qualification is saved per player; a general permission or OP status does not replace the Boss victory or reopen a completed trial. The Pokémon UUID, IVs and shiny result are reserved before delivery, preventing a fresh roll when retrying a pending claim.

Automatic delivery is verified with Cobblemon's supported native file-backed storage. Other storage providers leave the reward pending for review instead of risking an unverified duplicate.

**🔐 PLAYER AND ADMINISTRATOR PERMISSIONS**

Supports **LuckPerms** through NeoForge and the Bukkit permission bridge on Youer. Player nodes include **zianrct.medals**, **zianrct.battle**, **zianrct.rewards.claim** and **zianrct.legendary.claim**.

Editing NPCs, configuring trainers and rewards, changing progression and resolving uncertain deliveries use separate administrator permissions. Explicit denials also apply to operators. Without LuckPerms, administrative actions use the level-2 OP fallback.

**📜 USEFUL COMMANDS**

- **/medals** — Open the medal case; **M** is the default shortcut.
- **/zianrct npc** — Open the administrator NPC manager.
- **/zianrct reward pending** — List your pending trainer rewards.
- **/zianrct reward claim <operationId>** — Claim a pending reward; press Tab to select its ID.
- **/zianrct reward next <trainerId>** — Check reward availability.
- **/zianrct legendary status** — View your legendary trial status.
- **/zianrct legendary claim** — Claim a pending legendary reward or check an interrupted delivery.
- **/zianrct reload** — Reload the mod configuration.

**📦 INSTALLATION AND DEPENDENCIES**

Requires **Minecraft 1.21.1**, **Java 21**, **NeoForge 21.1.x**, **Cobblemon 1.8.1**, **RCTAPI 0.16.1-beta**, **Architectury 13.0.11** and **Kotlin for Forge 5.12.0**.

Install the **same Zian RCT version on both client and server**, with the required dependencies. **Do not install RCTMod alongside Zian RCT 0.2.x**; it is an incompatible dependency for this generation.

**AVECOINS 2.3/2.4** is optional for currency rewards. **LuckPerms** is optional for permission management. **Zian GUI** can provide a shortcut to the medal case.

**🔄 UPGRADING FROM AN OLDER RELEASE**

Back up the complete world, configuration and mod list before updating. Test a copied world, replace Zian RCT on both sides, remove RCTMod and retain RCTAPI and the other required libraries.

The compatibility layer reads supported legacy trainer NPC data and player progression, while retaining Zian medal, loot and legendary journals. Verify your existing NPCs and rewards before moving the real server.

This release does not include RCTMod's natural trainer spawning, trainer spawner blocks, association NPCs, unrelated series or its full trainer catalog. Those blocks/items and unsupported trainer definitions require separate migration planning.

**⚡ TRAINER LOADING AND PERFORMANCE**

Zian RCT loads the configured league and custom trainer definitions. Battle Pokémon teams are created when needed and released after the encounter. Saving trainer definitions updates the trainer catalog without forcing a global Minecraft resource reload.

Memory savings depend on the server, mods and workload; this release does not promise a fixed RAM reduction.

**🚧 BETA AND SUPPORT**

Gameplay testing on Youer covered Single and Double battles, progression and medals, candy caps, NPC persistence, rewards and waiting times, non-OP access, and a unique legendary award that remains closed after restarting.

For bug reports, provide mod versions, server platform, relevant logs and clear reproduction steps. Source code, administrator documentation and issue reports are available on GitHub.

**Created by IANBLK • Powered by RCTAPI by HDainester / hd42 • Original Zian code licensed under MIT**
