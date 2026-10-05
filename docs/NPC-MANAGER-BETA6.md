# NPC manager — Zian RCT 0.1.0-beta.6

## Open the list

Run `/zianrct npc`. The previous `/zianrct npc edit` command also opens the same manager.

The first screen lists **living RCT NPCs currently loaded in the administrator's dimension**, sorted by distance. It does not load remote chunks or recreate unloaded NPCs. Approach their area and refresh if a trainer is not listed.

Each row shows the trainer ID/name, a short UUID, block coordinates and distance. NPCs with the same trainer ID remain separate rows. The list has five rows per page with navigation.

**Modificar** selects that exact entity and opens its NPC/Loot editor. **Eliminar** opens a separate server-confirmed deletion screen. **Actualizar lista** reads the loaded entities again.

## Create an NPC

Choose **Crear NPC** to open a separate template browser. Search the trainer IDs loaded by RCT, choose one, then use **Crear el seleccionado** and **Confirmar creación**.

A successful creation opens the newly created NPC's editor, which has no spawn button. Consumed session tokens cannot create a second NPC, and edit screens reject spawn actions. A duplicate trainer ID within 16 blocks blocks creation and directs the administrator to modify the existing NPC. Intentional copies can be placed farther apart; their reward entitlement still shares the trainer ID.

New trainers appear about two blocks in front of the administrator, using horizontal facing even when looking up/down. Space, loaded chunks, world border and nearby-count guards remain enforced. Creation uses an existing RCT definition, preserving its team and battle rules.

## Modify, move or delete

The NPC editor contains separate NPC and Loot tabs. Permanence and movement controls are retained from beta.5.

**Mover a mi bloque y dirección** uses the administrator's current block centre on X/Z, exact feet height (including slabs), and horizontal yaw. This allows the administrator to move the selected loaded NPC to where they stand, within the same dimension. No client coordinates are accepted.

Solid blocks, dimension limits, world border and other living entities block placement. The administrator themselves is excluded from the overlap check because the requested destination is their block. The home position and any saved movement-lock anchor are updated, so a locked NPC does not return to its old location.

NPC movement/persistence/deletion is denied during battle. A missing or unloaded entity, changed trainer identity or mismatched session is rejected instead of modifying a different NPC.

Deleting requires **Eliminar / Confirmar eliminación**, with a fresh player-bound server token tied to the selected UUID. RCT's normal entity removal path is used. Saved medals, trainer progress, reward definitions and already earned reward claims are preserved.

If a native RCT spawner block or another mod is configured to create replacement trainers, removing its entity does not remove that spawner's configuration. The manager does not delete blocks or external datapack files.

## Loot

Editing loot retains the beta.4 journal rules and the beta.5 GUI: copy a held stack with its components, configure AVECOINS credits, or remove configured items/rewards.

Loot is **per trainer ID**, not per physical entity. Existing reserved rewards retain their original contents. Loot changes are blocked while a registered NPC with that trainer ID is battling.

See [trainer reward configuration and recovery](TRAINER-REWARDS-BETA4.md). Repeat cooldown rewards and new team creation are not added in this release.

## Permissions

The existing nodes remain:

`zianrct.admin.npc.edit` — Open/list/select and edit NPC settings.

`zianrct.admin.npc.spawn` — Prepare and confirm creation.

`zianrct.admin.rewards.configure` — Edit loot.

New nodes:

`zianrct.admin.npc.move` — Move a selected NPC to the administrator.

`zianrct.admin.npc.delete` — Prepare and confirm deletion.

Undefined admin nodes use OP level 2. Explicit LuckPerms denials are respected on each action. Example for an **existing** admin group, in console:

```text
lp group admin permission set zianrct.admin.npc.edit true
lp group admin permission set zianrct.admin.npc.spawn true
lp group admin permission set zianrct.admin.npc.move true
lp group admin permission set zianrct.admin.npc.delete true
lp group admin permission set zianrct.admin.rewards.configure true
```

## Compatibility and live checks

Install **beta.6 on server and clients**. The editor channel is now `npc2`; earlier editor clients are incompatible. The medal protocol and existing entity anchor keys remain unchanged, with no data migration required.

Automated tests cover single-use ownership/expiry, exact visible-page UUID selection, stage-specific create/delete confirmation rules, action bounds, and negative-coordinate block-centre placement. Existing reward, wallet, medal and permission coverage remains.

Live checks: distinguish the two existing duplicate NPCs by UUID/coordinates; open one and move it while facing another direction; verify a movement-locked NPC remains at its new anchor; delete only one duplicate; restart normally; verify the surviving trainer and earned progress. Test the list and scrolling at GUI scales 2/3. These live game checks are still required before publishing this beta.
