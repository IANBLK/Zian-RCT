# Changelog

## 0.1.0-beta.12

- Add default automatic moves for newly created independent trainers: choose species, level and difficulty. Easy/Normal draw from level-up moves available at that level; Hard/Boss also consider legal TM, tutor, egg and evolution moves. Rank attack strength, accuracy, same-type bonuses and type coverage; advanced presets reserve a support move when available.
- Recompute automatic teams authoritatively on server save, persist the resulting moves, and keep manual mode/selectors available. Existing definitions without the optional autoMoves flag stay manual and retain their moves. Changing difficulty/level in automatic mode updates the set on the next save.
- Retain beta.11's dialogue crash fix, all six skins, difficulty IVs/equipment and reward histories. Require matching beta.12 clients/server via editor channel npc6. The heuristic is an initial team preset, not a competitive optimisation guarantee.

## 0.1.0-beta.11

- Fix independent trainer dialogue generation for RCT speech bubbles. Provide both literal text and a non-null translatable lookup value, using the displayed text itself so client/server need no static translation entries for administrator-edited dialogue.
- Address the initiating NullPointerException in RCT's speech queue at battle start/end, which caused Cobblemon's secondary Showdown cleanup crash. Keep the battle engine, outcomes and reward journals unchanged.
- Regenerate dialogues for existing saved independent definitions automatically; no NPC recreation or configuration reset is needed. Keep editor channel npc5 and all beta.10 selectors, skins and difficulty settings.

## 0.1.0-beta.10

- Add searchable, paginated Pokémon and per-species move selectors to the independent team editor. Names follow the client's Cobblemon language; move tooltips show descriptions and combat values. Select up to four moves from the standard form's legal learnset, including TM/tutor moves regardless of level. Manual entry remains available.
- Keep selector changes in the creator draft until the administrator explicitly saves. Cancel preserves the draft; changing species selects up to four available level-up moves for that member only.
- Replace the four dependency skin choices with six unchanged administrator-supplied 64×64 textures bundled for every client: Explorador Ártico, Centinela Nocturno, Guardián Cian, Aventurero del Desierto, Guardabosques and Capitán Ámbar. Existing numeric skin choices 0–3 now refer to the first four replacements.
- Set difficulty IVs to 10 / 20 / 31 / 31. Easy has no held item; Normal uses Oran Berry, Hard Sitrus Berry and Boss Leftovers. Retain existing level, AI and legal EV presets. Changes apply to independent definitions when regenerated, including existing definitions.
- Require beta.10 on server and clients (editor channel npc5), and add regression checks for difficulty output and all six bundled skin textures.

## 0.1.0-beta.9

- Show visible trainer names and readable return times in repeated reward delivery messages; add Shift/right-click availability queries and a clickable availability link in Rassvet invitations.
- Add a permission-protected independent trainer creator with own IDs, names, difficulty presets, 1–6 Pokémon/levels/moves, single/double format, installed-skin selection and contextual dialogues.
- Generate optional standalone challenge resources outside the Rassvet chain, with no league prerequisites or automatic natural spawning. Preserve existing medals, player series selection and reward history.
- Persist owned definitions atomically and validate species/moves and identity bounds before publishing; prevent overwriting external trainer IDs or changing definitions during active battles/reloads.
- Synchronize installed skin references and extend the public RCT renderer without copying dependency assets. Require editor channel npc4 on clients/server.
- Add independent creator/resource/message tests and a dedicated-server fixture for an owned double-format trainer. Legendary encounter unlocks and arbitrary skin import remain later stages.

## 0.1.0-beta.8

- Add unique/repeated reward policy and per-player wait settings in the NPC Loot GUI, with personal eligibility queries and pending-cycle blocking.
- Persist separate reward cycles, frozen waits and actual battle IDs. Migrate legacy claims without changing their original IDs or replaying completed rewards.
- Enable native RCT rematches for repeated challenges through verified virtual datapack overrides while retaining prerequisites and relative caps.
- Add Individual/Double selection in the NPC editor, preserving the trainer's team and other battle settings. Require `zianrct.admin.npc.configure` for this shared-ID format change.
- Keep changes blocked during active battles and verify requested native rules after reload. Require beta.8 on client/server with editor channel npc3.

## 0.1.0-beta.7

- Autocomplete `/zianrct reward claim <operation>` with the executing player's actionable pending reward IDs, filtered by the typed prefix.
- Add a clickable `[Reclamar]` action to pending reward chat entries. Completed or review-blocked rewards are not offered for claiming.
- Recheck claim permissions for suggestions; preserve ownership checks, one-time delivery and the existing NPC/medal protocols.

## 0.1.0-beta.6

- Open a paginated loaded-NPC list with `/zianrct npc`, showing UUIDs, coordinates and distance; retain `/zianrct npc edit` as a route to the same manager.
- Separate template creation from editing an existing entity. Require server-confirmed creation and block nearby duplicate trainer IDs.
- Add UUID-bound confirmed deletion through the NPC list/editor without removing player progress or earned reward history.
- Add a separately permission-protected move action to the administrator's block centre, feet height and facing. Update native home position and saved movement anchors.
- Recheck action stages and loaded entity identity, protect ongoing battles, and add `zianrct.admin.npc.move` / `zianrct.admin.npc.delete`.
- Require editor channel `npc2` on server and client; preserve medal protocol and saved entity/reward data.

## 0.1.0-beta.5

- Add an in-game NPC/Loot editor through `/zianrct npc edit`, with trainer-ID search, selection of nearby existing NPCs and creation from loaded RCT trainer definitions.
- Add native RCT persistence controls and per-entity movement locks with saved anchors, paused during active battles.
- Configure copied held-item rewards and AVECOINS credits in the GUI. Preserve the one-time per-player/trainer journal and frozen claims from beta.4.
- Recheck delegated LuckPerms permissions on every server action, bind single-use tokens to the administrator, validate nearby targets and enforce payload/rate limits.
- Add a required independent editor network channel; beta.5 is required on server and clients. Existing medal protocol, progress and trainer teams are preserved.

## 0.1.0-beta.4

- Add configurable one-time rewards per player UUID and RCT trainer ID, independent from medals and historical reconciliation.
- Administrators can copy held item stacks with their components and configure an optional AVECOINS 2.3/2.4 credit using permission-protected commands.
- Freeze rewards at victory, persist per-component delivery intents, retain pending rewards for full inventories/wallets and skip completed components on retries.
- Keep interrupted or uncertain external deliveries blocked for evidence-based administrative review; never replay an uncertain credit or item delivery automatically.
- Save and verify player inventory data before acknowledging item components. Invalid reward configuration/journals disable reward mutations without replacing their evidence.
- Add optional AVECOINS metadata, dedicated reward permissions, player pending/claim commands and administrative review/confirmation commands.
- Preserve existing medal progress, artwork, protocol 2 and native RCT progression. Repeat cooldown rewards and the graphical trainer editor remain future stages.

## 0.1.0-beta.3

- Display the ten Rassvet badges using the original pixels of the approved medal render, embedded as a source atlas; retain medal IDs, earned progress and protocol 2.
- Center a content-sized gold-and-charcoal medal panel, removing the large unused lower area. Add a matching Close button and pagination for small GUI resolutions or larger custom collections.
- Preserve locked/obtained states, metadata tooltips, and custom texture paths. Existing default texture paths select the new atlas artwork; alternate texture paths still use their configured images.
- Add regression coverage for panel bounds/pagination and source atlas regions. Install beta.3 on the client and server; no configuration or saved-ledger migration is required.

## 0.1.0-beta.2

- Add optional LuckPerms permission checks for medal viewing, league challenges and each administrative action. Support both NeoForge and Youer/Bukkit installations.
- Respect explicit permission denials even for operators; deny access when an installed provider cannot be queried. Keep console administration and defaults when LuckPerms is absent.
- Stop league challenges if prerequisite progress cannot be verified.
- Remove expired invitations and clear them on logout and server stop.
- Flush medal writes before replacing the saved ledger; reject malformed null records as persistence errors without rewriting the source file.
- Restore the missing Gradle launcher scripts and wrapper JAR, and run CI on review branches and pull requests.
- Keep the existing medal protocol and world/configuration data formats.
