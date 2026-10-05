# Changelog

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
