# Repeated challenges and battle format — 0.1.0-beta.8

## GUI

Open `/zianrct npc`, choose **Modificar**, and open **Loot**.

Toggle **Tipo de premio** between Único and Repetible, set the wait in minutes and press **Guardar tipo**. Default displayed repeat wait is 1440 minutes (24 hours); valid repeat waits are 1–43200 minutes (30 days). Item and coin edits preserve this policy.

**Para ti** shows eligibility for the administrator viewing the menu. **Actualizar mi espera** reads it again. Normal players can query their own status with:

```text
/zianrct reward next <trainerId>
```

The **NPC** tab offers **Batalla: Individual / Doble (cambiar)**. This modifies the native team definition's battleFormat, shared by trainer ID, and requires `zianrct.admin.npc.configure` in addition to editor access. It preserves Pokémon, moves, stats, AI and other battle rules.

Policy and format changes trigger a controlled RCT resource reload. Do not change them during battles against that ID. Until RCT verifies the requested settings, new interactions are held back. Inspect server logs if verification fails.

This beta supports these overrides for trainers with individual datapack definitions. The built-in Rassvet trainers are supported. Group-only/fallback mob definitions are refused before policy changes rather than replacing their inherited rules with guessed defaults.

## Repeated rewards

Cooldown is per player UUID and trainer ID, shared across physical copies of the same trainer. It starts when a legitimate victory reserves a reward. Losses do not reserve a reward or start its reward wait. Native short battle cooldowns and progression prerequisites remain.

Repeat mode sets native maxTrainerDefeats and maxTrainerWins to unlimited for that trainer, so RCT permits later attempts. The interaction gate enforces the reward wait and requires the previous reward to be fully settled before another rewarded challenge. Existing medal grants remain idempotent.

Each cycle has a distinct operation UUID and freezes its prize and wait. The original cycle-0 UUID remains unchanged. A later policy edit does not shorten a wait already recorded for the last won cycle. The next victory after that interval uses the new definition.

The actual Cobblemon battle UUID is recorded. A duplicate victory callback for the same player/trainer/battle cannot create another paid cycle, even after cooldown expiry.

Pending or uncertain components block the next cycle. Claims and administrative review retain the prior journal safeguards; no paid record is reopened or deleted to implement repetition.

Native RCT loot/commands from other definitions remain independent of Zian rewards. Review overlapping native payouts before enabling repeated battles.

## Commands and permissions

```text
/zianrct reward policy <trainerId> unique
/zianrct reward policy <trainerId> repeat <minutes>
/zianrct reward next <trainerId>
```

Policies require `zianrct.admin.rewards.configure`; personal next queries use `zianrct.rewards.claim`.

The format GUI uses `zianrct.admin.npc.configure`. Example for an existing admin group:

```text
lp group admin permission set zianrct.admin.npc.configure true
```

Native format overrides are saved in `config/zianrct-trainer-options.json`. Reward definitions and journal now write schema 2. Old schema-1 definitions become UNIQUE without changing amounts/items, and original paid/pending claim IDs are retained. Do not downgrade to an older reward implementation after schema-2 files have been written.

Turning an existing unique reward into REPEAT is an explicit administrative policy change: a new legitimate victory may earn a new cycle after the previous claim is fully settled. Switching back to UNIQUE prevents further cycles while retaining all history.

Install beta.8 on server and all clients: editor channel is now npc3. Medal protocol remains 2.

## Validation

Automated tests cover schema-1 migration, distinct cycle IDs, pending/uncertain blocking, frozen cooldowns, battle replay, prize edits preserving policy and safe native JSON overrides. Bounds remain 20,000 claims/16 MiB per world; history is not automatically pruned and a full journal refuses further writes. Keep backups when upgrading.

Live test in a disposable world: set REPEAT with 1 minute, win and verify one reward; retry before expiry and observe the wait; wait or restart until it expires, then win a second real battle and verify a distinct operation with no extra medal/cap increase. Test a full-inventory pending prize and ensure a second cycle is blocked until settled.

For doubles, configure a supported trainer with at least two usable Pokémon, use a player party with at least two usable Pokémon, choose Doble and wait for reload verification. Start a real battle and check the two active slots. The native runtime—not unit tests—must validate real double battle behavior and AI.

## Planned trainer creator

A later editor will create new IDs and their own trainer/mob/dialog definitions, rather than replacing RCT's original files. Proposed sections: name/identity and access requirements; a Pokémon team with levels, moves, ability/nature/IVs/EVs/held items; difficulty presets exposing their actual settings; individual/double format; appearance from installed textures or a distributed client resource pack; contextual dialogues; and loot/repeat policy.

Custom skins are client resources. Saving a PNG only on the server is insufficient for clients to see it. The creator, new teams, skin import and dialogue editing are not implemented by this beta.

Primary documentation: [trainer format and team fields](https://srcmc.gitlab.io/rct/docs/latest/configuration/data_pack/trainers/), [skins](https://srcmc.gitlab.io/rct/docs/latest/configuration/resource_pack/textures/), [dialogues](https://srcmc.gitlab.io/rct/docs/latest/configuration/data_pack/dialogs/).
