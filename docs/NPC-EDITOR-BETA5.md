# NPC and loot editor — 0.1.0-beta.5

Open `/zianrct npc edit` as an administrator. The editor initially selects the closest living RCT trainer within 8 blocks, if present. Otherwise it opens a trainer template.

## NPC tab

Search the IDs loaded by RCT, then click a matching ID or **Usar ID**. Selecting an ID chooses a template; it does not replace the team or identity of an existing NPC.

**NPC cercano** selects the closest existing trainer in the same dimension, within 8 blocks.

**Hacer aparecer delante de mí** creates a trainer from the selected existing RCT definition about two blocks in front of the administrator. Clear solid obstacles before spawning. The server checks collision, loaded chunks, world bounds and a limit of 32 nearby trainers within 32 blocks.

New NPCs are persistent and movement-locked by default. The native RCT spawner registration and persistence API are used. This editor does not create new trainer teams or bypass battle prerequisites.

**Permanente** toggles RCT's persistence flag for that selected entity. Leaving it disabled restores native RCT despawn/save rules; it may disappear. Persistence is not an invulnerability switch.

**Movimiento** enables normal AI or locks the NPC to its current position. A lock records an anchor in that entity's persistent data, stops navigation, disables AI, and restores the anchor after external pushes when not battling. The anchor is suspended during an active battle. Unlock movement before relocating a trainer through another command. The prior gravity flag is restored when releasing an editor-owned lock.

Editing a live NPC requires it to remain nearby, alive, in the same dimension, and with the same trainer ID. Persistence/movement changes are rejected while it is fighting.

## Loot tab

Rewards are configured per **trainer ID**, not per physical NPC. Two NPCs with the same trainer ID share the same reward definition and one-time eligibility. Existing reserved rewards are frozen and unaffected by edits.

Set an AVECOINS currency and amount, then use **Guardar monedas**. Amount 0 removes currency payment. Supported provider versions and limits remain those documented in [the reward guide](TRAINER-REWARDS-BETA4.md).

Hold the intended stack in the main hand and use **Añadir stack**. The server reads and copies the actual stack with its count and components; clients cannot submit fabricated item data through the editor.

The list displays the saved stacks. **Vaciar objetos** clears configured items while retaining coins. **Quitar loot** removes the trainer's reward definition. Both destructive buttons require a second click. Neither removes previously earned claims.

Loot changes are rejected while the selected NPC or another RCT-registered NPC with the same ID is battling. The definition active when a victory is processed becomes the frozen reward.

The UI has NPC/Loot tabs, scrolling for small GUI scales, a themed Close button and status messages. Use the mouse wheel if controls extend below the visible area.

## Permissions

`zianrct.admin.npc.edit`: open the editor, search/select existing trainer definitions and control nearby NPC persistence/movement.

`zianrct.admin.npc.spawn`: additionally required to make an NPC appear.

`zianrct.admin.rewards.configure`: additionally required to change loot.

Undefined administrative nodes fall back to OP level 2. Explicit LuckPerms denials are respected. These permissions can be delegated through groups on NeoForge or Youer/Bukkit. Example for an existing administrator group:

```text
lp group admin permission set zianrct.admin.npc.edit true
lp group admin permission set zianrct.admin.npc.spawn true
lp group admin permission set zianrct.admin.rewards.configure true
```

Every action rechecks permissions on the server and uses a player-bound, single-use session token. Requests are throttled, expire after five minutes and become invalid after reopening the editor or disconnecting. If a session expires, open the command again. Successful actions refresh the server snapshot.

## Update and validation

Install **beta.5 on both the server and every client**. Medals retain protocol 2; the editor adds a required independent `npc1` channel, so older clients lack its payloads.

Entity settings use native Minecraft/RCT saving and entity persistent data, rather than a replacement NPC database. Normal world save/restart preserves persistent NPCs and movement anchors. No file migration or medal-ledger reset is needed.

Live checks: open without/with the three permissions; select a trainer template; create it with space in front; confirm it remains fixed; unlock and relock movement; change loot using a named/enchanted held stack; restart normally and inspect the same NPC. Test a real victory to verify medals, native battle rules and frozen one-time rewards. Repeat at GUI scales 2 and 3.

Automated coverage includes session ownership, replay, expiration, reopening and packet bounds, plus all existing medal/permission/reward tests. In-game NPC appearance, movement/battle interactions and RCT persistence still require operator validation.
