# Trainer rewards — Zian RCT 0.1.0-beta.4

## Scope

This first stage adds **one reward per player UUID and exact RCT trainer ID**. Items and optional AVECOINS credits are independent from medal ownership, revocation, cap changes and historical reconciliation. It supports any loaded RCT trainer ID, including trainers outside the configured medal chain.

The existing RCT victory event is the only automatic trigger. Forced battle endings, losses, historical victories and administrative medal grants do not create rewards. A later legitimate victory can earn a newly configured reward if no reward claim exists for that player/trainer.

This release does not add repeat cooldown rewards, a graphical trainer editor, new NPC teams, or legendary encounters. Native RCT win commands or rewards still execute independently; remove overlapping native rewards if using this module instead.

## Configure inside the game

Rewards start with an empty configuration: no currency or items are awarded until an administrator configures them. Definitions are stored at `config/zianrct-rewards.json`; commands save changes immediately without a restart.

Hold the desired stack in your main hand:

```text
/zianrct reward add-item rassvet_leader_novato
```

The whole held stack, including its count and components (enchantments, custom name, etc.), is copied without consuming it. Repeat to add another stack, up to 8 per trainer.

Add currency:

```text
/zianrct reward set-money rassvet_leader_novato avecoins:coppercoin 5
```

Set the amount to 0 to remove that credit while retaining items. The maximum configured credit is 1728 currency units per trainer, matching the inspected wallet capacity. Credits respect the capacity occupied by other currencies.

```text
/zianrct reward list
/zianrct reward clear-items rassvet_leader_novato
/zianrct reward remove rassvet_leader_novato
/zianrct reward enabled false
/zianrct reward enabled true
/zianrct reward reload
/zianrct reward status
```

`enabled false` stops new reservations. Previously earned pending rewards remain claimable. Removing or editing a definition does not delete claims or reopen completed rewards.

Reward definitions are frozen when the victory creates the claim. Editing rewards does not change an already earned pending reward. Trainer names in these commands are RCT IDs, not the visible badge names. A trainer must exist in RCT before configuring it through commands.

## Player claims

Normal victories reserve the reward and attempt delivery. A full inventory, missing item dependency, absent AVECOINS, unsupported currency or full wallet leaves the relevant component pending.

```text
/zianrct reward pending
/zianrct reward claim <operation-uuid>
```

From beta.7, type `/zianrct reward claim ` with a trailing space and press **Tab** to select one of your actionable pending IDs. You can also click **[Reclamar]** beside a pending entry. Completed rewards, rewards belonging to other players and review-blocked components are excluded from suggestions. Clicking a stale completed entry does not deliver again.

Claim IDs belong to the player; another player cannot claim them. Rejoining displays a pending-reward reminder, without automatically replaying deliveries. Restore required item mods or the wallet provider before retrying unavailable components.

Manual claims are limited to one attempt per player per second. Pending output shows up to 20 operations at a time; after claiming those, query again for the remaining entries.

AVECOINS 2.3 and 2.4 are supported through the same inspected wallet contract used by Zian Utilities, copied under MIT into this mod. Zian Utilities is not required. Future AVECOINS versions are not accepted automatically. An absent economy provider does not prevent item-only rewards or medals.

## LuckPerms

`zianrct.rewards.claim`: list and claim personal pending rewards; allowed by default without an explicit deny.

`zianrct.admin.rewards.configure`: configure, remove, enable and reload definitions.

`zianrct.admin.rewards.review`: inspect runtime status and player claims.

`zianrct.admin.rewards.resolve`: confirm independently verified deliveries or compensation.

Administrative nodes require operator level 2 when undefined. Explicit denials apply to operators. LuckPerms can be a NeoForge mod or a Bukkit plugin on Youer. Commands execute through Zian RCT's server-side permission checks, not temporary OP or arbitrary console commands.

Console examples (omit the slash):

```text
lp group default permission set zianrct.rewards.claim true
lp group admin permission set zianrct.admin.rewards.configure true
lp group admin permission set zianrct.admin.rewards.review true
lp group admin permission set zianrct.admin.rewards.resolve true
```

Use an existing administrator group; do not grant administrative nodes to default players.

## Persistence and interrupted delivery

Claims are stored in the world's `data/zianrct-rewards.json`. Do not delete this file to fix a pending reward: deletion removes the one-time history.

Every component is durably marked APPLYING **before** touching the wallet or inventory. Delivered components are never replayed. A wallet-full rejection is retryable because capacity is checked before mutation. An unconfirmed wallet save becomes REVIEW_REQUIRED.

Item delivery checks available space, uses the normal inventory insertion, saves that player's data through Minecraft's normal save path, flushes it and verifies the saved inventory before acknowledging delivery. The access transformer widens the normal save method, including its integrated-server override, without changing its behavior.

There is no shared atomic transaction across the reward journal, Minecraft player data and AVECOINS. Interrupted APPLYING or uncertain components therefore remain blocked after restart. They are not assumed to have failed. An unreadable journal or failed journal write prevents further reward mutations rather than resetting the history; medals remain independent.

Inspect an online player:

```text
/zianrct reward review <player>
```

The output shows operation IDs and component numbers starting at 1.

After confirming the original component was received:

```text
/zianrct reward confirm-delivered <player> <operation-uuid> <component-number> <evidence>
```

If a component is verified missing, manually deliver only the verified missing portion or agreed compensation first, then record it:

```text
/zianrct reward confirm-compensated <player> <operation-uuid> <component-number> <evidence>
```

Evidence is a required single-line reference of at most 160 characters. These commands record the administrator and decision; they do not issue currency/items, prove receipt, or reset a component to retry. Only interrupted/uncertain components can be resolved this way. Once reviewed, the player may claim remaining pending components.

Limits: 256 reward definitions, 8 item stacks plus one currency credit per definition, 1 MiB configuration, 16 MiB/20,000 claims per world. Old completed history is never pruned automatically. Preserve backups and review logs if a limit is reached.

## Live test before production

Use a disposable world or a trainer/account eligible for a new victory. A leader already defeated may not permit a rematch under native RCT rules; use the next eligible trainer or another test account.

1. Configure one held item stack and 5 coppercoin before the battle.
2. Defeat the trainer: verify its medal, item count and wallet increase.
3. Repeat an eligible battle or query/claim the same completed operation: no second reward.
4. Repeat with another account: its entitlement is independent.
5. Fill the inventory before winning: a claim remains pending; free space and claim it.
6. Restart with a pending claim, then claim: it remains available; completed components are not repeated.
7. Edit the definition after earning a pending claim: the existing claim retains its original contents.
8. Confirm a player without administrative permissions cannot configure or resolve rewards.

Automated tests cover unique reservation, frozen definitions, restart persistence, capacity deferral, partial delivery, concurrency, ownership, interrupted intents, verified manual resolution, failed writes, corruption, wallet-full behavior and unconfirmed wallet saves. A real battle with live AVECOINS and forced-stop player-data boundaries still requires operator testing; unit tests do not prove every server/plugin combination.
