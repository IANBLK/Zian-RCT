**✨ ZIAN RCT — 0.1.0-beta.15**

**🛠️ TRAINER CREATION AND NPC MANAGEMENT**

This beta includes an in-game manager for loaded NPCs, with creation, editing, confirmed deletion, persistence, movement control, and placement at the administrator's current block and facing.

Create independent trainers with names, teams, levels, difficulty, Single or Double battle format, six included skins, and custom dialogue.

Search Pokémon and their moves, or use automatic move selection based on species, level, and difficulty. Hard and Boss battle presets use perfect IVs and automatic held items.

**🎁 TRAINER REWARDS**

Configure item and AVECOINS rewards with one-time or repeatable eligibility and a per-player waiting period.

Added readable trainer names and return times, pending reward autocomplete, clickable claim actions, and availability checks.

Reward progress and completed deliveries persist across restarts. Interrupted or uncertain external deliveries remain blocked for review.

**🐉 UNIQUE LEGENDARY TRIALS**

Link a Boss trainer to a legendary custodian through the Trial / Prueba tab.

A real Boss victory unlocks the second encounter for that player. Winning the custodian battle delivers one Pokémon to the PC with frozen UUID, random IVs from 25–30, and configurable shiny probability, defaulting to 1/512.

Losing allows retry. Confirmed delivery closes the trial permanently for that player. Native file-backed PC/party readback verifies the saved Pokémon before acknowledging the reward.

**💬 FIXES IN BETA 15**

Fixed missing independent-trainer interaction dialogues for cooldown, busy battles, missing Pokémon, level-cap restrictions, unavailable encounters, and fallback requirements.

Independent trainers now explain their actual refusal reason instead of inheriting an unrelated league-series refusal. Above-cap messages show the player's current limit.

Saved trainer dialogues regenerate automatically when loading the updated mod; existing NPCs do not need to be recreated.

**🔧 OTHER FIXES INCLUDED**

Fixed the independent-trainer speech dialogue failure that caused errors at battle start or completion.

Corrected legendary storage confirmation to inspect persisted Pokémon directly rather than relying on an uninitialized UUID lookup cache.

Completed or merely unlocked legendary trials remain silent on login. Notifications are limited to pending, in-progress, or review deliveries. The manual status command and completed-NPC interaction message remain available.

**🔐 PERMISSIONS AND SAVED DATA**

Retains LuckPerms/Youer checks and separate permissions for editing, trial configuration, and administrative recovery.

Preserves medals, trainer definitions, trial eligibility, frozen reward data, and completed reward history. OP does not bypass completed trials or native team level caps.

**📦 UPDATE REQUIREMENTS**

Install **0.1.0-beta.15 on both client and server**. The editor remains on protocol **npc7**.

This beta targets Minecraft 1.21.1, NeoForge, Cobblemon 1.8.1, RCTMod 0.19.2-beta, and RCTAPI 0.16.1-beta, with Architectury and Kotlin for Forge.

**✅ VALIDATION**

Passed **113 automated tests** and **GitHub build 124**, including native trainer/dialogue loading, legendary PC delivery/readback, replay protection, and reload checks.

Gameplay testing on Youer confirmed Boss qualification, losing and retrying, legendary receipt, blocked repetition, and persistence after restarting.

**This is a beta release.**
