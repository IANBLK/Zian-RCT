# RCTAPI standalone migration (beta)

The 0.2.0-beta.1 release replaces RCTMod with original Zian implementations. RCTAPI
0.16.1-beta, Cobblemon 1.8.1, NeoForge 1.21.1, Architectury and KotlinForForge remain
separate required dependencies. Do not install RCTMod alongside this branch.

## Implementation

- Own `zianrct` RCTAPI instance, NPC entity, renderer and player progression ledger.
- Loads configured league trainers and Zian custom definitions, not the upstream
  catalog. Pokemon teams are created for battles and unregistered after completion.
- Trainer edits update the catalog directly, without invoking Minecraft's global
  resource reload. A manually requested `/reload` still reloads Minecraft normally.
- Existing reward and legendary journals, trainer IDs and custom skin selections
  are retained. Progress is written atomically before publishing victory rewards.
- Legacy `zianrct:independent_trainer` and `rctmod:trainer` entities keep compatible
  ID, home position, cooldown and persistence fields. The latter is a compatibility
  registration implemented by Zian, not an embedded copy of RCTMod.
- On first load of each player's progress, legacy `rctmod.player.<uuid>.stat.dat`
  files are read without modification. A new ledger then becomes authoritative;
  subsequent restarts never re-import an old snapshot over newer progress.
- League prerequisites and party caps apply to league NPCs. Independent challenges
  do not require joining Rassvet. League XP/candy caps apply from the first entry, including the initial cap
  before defeating any trainer. Existing
  Pokemon above the cap are never downgraded.

## Upgrade procedure

1. Stop the test server and copy the entire world, configuration and mod list.
2. Test the copied world first. Replace ZianRCT on both sides, remove RCTMod on both
   sides and keep the separate RCTAPI dependency and the other required libraries.
3. Verify NPC positions, dialogs, skins, party caps, previous medals, repeat rewards,
   previously closed legendary trials, new battle victories and a clean restart.
4. Only update the real server after the copied-world checks pass. To roll back,
   restore the complete backup, including configs, journals and mods together.

This branch does not reproduce RCTMod's natural spawning, trainer spawner blocks,
association NPCs, unrelated trainer series or upstream catalogs. Previously saved
NPCs referencing definitions outside the configured league/custom catalog cannot
battle until their definitions have been recreated. Removing RCTMod also removes
its blocks/items; do not use this beta on the only copy of an existing world.

## Licensing

RCTAPI and RCTMod publish MCOML v1 (21 March 2026):

- https://www.curseforge.com/minecraft/mc-mods/radical-cobblemon-trainers-api/license
- https://www.curseforge.com/minecraft/mc-mods/rctmod/license

MCOML permits copying/modifying source subject to its licensing, attribution and
change-disclosure conditions. Publishing derivative binaries also requires its
meaningful-change criterion (at least 50% code or functionality), or explicit
written permission from all project owners, plus public source and documentation.
There is no assumption here that counting lines establishes compliance.

This implementation instead uses public RCTAPI contracts as a separate dependency.
No RCTMod/RCTAPI implementation source, binaries or upstream assets are bundled in
the ZianRCT JAR. Original Zian source remains under its existing MIT license. Public
API use and reading the legacy save-file contract are not claims of ownership over
RCT's work. RCTAPI/RCTMod authors retain their rights and their own licenses.

Before incorporating upstream code/assets in a future change, review the exact
version's license, document the material and address the derivative-work obligations
or obtain written permission. CurseForge approval is separate from these conditions.
