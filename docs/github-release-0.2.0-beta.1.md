**✨ ZIAN RCT — 0.2.0-beta.1**

**🚀 A NEW GENERATION POWERED BY RCTAPI**

Zian RCT now runs with **RCTAPI and Cobblemon without requiring RCTMod**. Added Zian-owned NPCs, rendering, trainer loading, progression and battle integration while retaining the existing editor, medal interface, loot and legendary reward systems.

Only the configured league and custom definitions are loaded. NPC battle teams are created on demand and released when the encounter ends. Trainer edits update the catalog without triggering a global Minecraft resource reload.

**📈 FIXED LEVEL CAPS FROM THE FIRST ENTRY**

The initial cap now applies before winning any league challenge. XL Candies, Rare Candies and experience from battles, commands and other mods using Cobblemon's XP system cannot raise a Pokémon beyond its unlocked level.

Training resumes after the next cap is unlocked. Candies rejected at an already reached cap remain unconsumed. OP status does not bypass these checks. Existing above-cap Pokémon are not forcibly downgraded.

Default league progression remains **10 → 20 → 30 → 40 → 50 → 60 → 70 → 80 → 90 → 100**. Aurelia remains the final medal challenge after the maximum cap has been unlocked.

**🎨 TEN LEADER SKINS**

Added dedicated skins for Novato, Ferrum, Aquila, Voltar, Engranaje, Bruma, Cognitus, Forjax, Glacius and Maestra Aurelia. The supplied PNG files are included unchanged and assigned through stable trainer IDs, so existing league NPCs receive the corresponding appearance.

Retains six selectable custom-trainer skin presets and adds Zian-owned overhead dialogue rendering.

**🛠️ NPC AND TRAINER MANAGEMENT**

Retains in-game creation, loaded-NPC browsing, editing, moving to the administrator's block/facing, persistence, movement control and confirmed deletion.

Includes Pokémon/move selectors, Single and Double battles, automatic moves, difficulty-based AI/IV/EV/held-item presets and custom dialogues. Independent trainers remain separate from league prerequisites.

**🏆 PROGRESSION AND SAVED DATA**

Added a durable player progression ledger with atomic writes and battle-event replay protection. Real battle victories are tracked separately from administrative cap changes, preventing cap adjustments from automatically counting as medal victories.

Retains medal ownership, configured rewards, cooldown eligibility and legendary delivery records. Supported legacy trainer NBT and progression can be imported without modifying the source files; newer progress is not overwritten by re-importing old snapshots.

**🎁 REWARDS AND LEGENDARY TRIALS**

Retains item/component rewards, optional AVECOINS payments, one-time rewards, repeatable rewards with waiting times, pending claim autocomplete and administrative review of uncertain deliveries.

Boss victories unlock a linked legendary custodian per player. A custodian victory awards one Pokémon to the PC with reserved UUID, IVs from 25–30 and a configurable shiny chance, defaulting to 1/512. Losses allow retry; confirmed delivery permanently closes the trial.

**✅ VALIDATION**

Passed **124 automated tests** and isolated dedicated-server checks covering Single/Double battle startup, actual winner callbacks, legendary storage confirmation, replay protection, invalid configuration rejection, reload and restart persistence.

Native XP/item checks covered XL Candies, Rare Candies, battle/command/sidemod experience, preservation of rejected candies and resumed training after unlocking the next cap.

User gameplay testing on Youer confirmed progression from Novato and Ferrum, medal and cap persistence after restart, caps respected by candies, Double battles, repeatable reward timing, non-OP challenges and a legendary reward that cannot be received again after restarting.

**📦 UPDATE REQUIREMENTS**

- Install **0.2.0-beta.1 on both client and server**.
- Keep **Cobblemon 1.8.1, RCTAPI 0.16.1-beta, Architectury 13.0.11 and Kotlin for Forge 5.12.0**.
- Use **Minecraft 1.21.1 / NeoForge** and Java 21.
- **Remove RCTMod** on both sides for this release.
- Back up and test a copied world before migrating older installations that depend on RCTMod content.

RCTMod natural spawning, spawner blocks, association NPCs and unrelated trainer catalogs are not reproduced by Zian RCT. See the migration guide for compatibility limits.

**Release channel: BETA • Client and server required • Console monitoring is separate from this mod**


## Downloads

Use `zianrct-0.2.0-beta.1.jar` on both client and server. Keep RCTAPI as a separate required dependency.
