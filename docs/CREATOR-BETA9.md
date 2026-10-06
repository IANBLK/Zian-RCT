# Independent creator and player reward information — beta.9

## Player information

Repeating reward delivery now uses the trainer's visible name, e.g. **Recompensa repetible del entrenador Novato entregada. Vuelve dentro de 3 horas para recibir otra recompensa.**

Durations use singular/plural Spanish and rounded readable minutes/hours. The journal's actual deadline remains authoritative.

**Shift + right-click** a configured reward trainer to view your own next availability without starting a battle. The existing Rassvet invitation also has **Ver disponibilidad de recompensa**. The typed command remains `/zianrct reward next <id>`. Ordinary interactions during a wait identify the trainer and time remaining.

A stale paid chat claim cannot replay delivery, and now explicitly reports that it was already delivered, using the latest eligibility status.

## Define a trainer from zero

Open `/zianrct npc` → **Crear NPC** → **Definir entrenador desde cero**.

The three tabs are:

**Identidad:** an ID beginning with `zian_custom_`, visible name, difficulty, individual/double format and one of four installed RCT skins.

**Equipo:** up to six species, levels from 1 to 100 and 1–4 moves per member. Leave a species field empty to omit that slot. Example species `pikachu` or `cobblemon:pikachu`; example moves `tackle,thunderbolt`. The server checks species/move registries before saving. Double format requires at least two members.

**Diálogos:** start, player wins and player loses. Each phrase supports up to 256 characters. No commands are accepted as dialogue or win actions.

Difficulty presets expose a simple initial balance: FACIL uses IV 10/EV 0 and more random AI; NORMAL IV 15/EV 0; DIFICIL IV 25/EV 64 per stat; JEFE IV 31/EV 84 per stat. The AI selection margin decreases and switch bias increases through these presets. Pokémon levels and chosen moves remain explicit.

Use **Guardar definición**, wait for resource verification, then create the selected NPC through the normal confirmation flow. A new definition cannot silently overwrite an existing RCT/datapack ID or another owned ID. Select an existing own NPC and use **Editar definición propia** to update that definition; its ID is locked.

## Independence and persistence

Definitions are atomically saved in `config/zianrct-custom-trainers.json`, with limits of 64 owned trainers and 1 MiB. Generated team, mob and dialogue resources are separate from the embedded Rassvet chain.

Own trainers are optional members of a separate `zian_challenges` collection, with no required Rassvet defeats/series and no automatic natural spawning. They are not included in Zian RCT's medal chain and do not add medals. The creator does not switch a player's current RCT series or bypass their server's global Pokémon level limits.

Their own relative cap offsets the enemy team maximum, removing a league-specific minimum access cap. The independent collection starts at cap 1 if explicitly chosen through native RCT tools; creating or fighting an NPC does not itself switch the player into that collection. Live checks of RCT progression and access remain necessary.

Owned NPCs use the registered entity `zianrct:independent_trainer`, a subclass of the public RCT trainer class. It permits a normal native battle without requiring the player's current series to match the independent collection, while retaining native busy/cooldown checks and the player's global Pokémon level cap. This behavior survives entity save/reload. Native RCT may still record the foreign defeated ID in its battle bookkeeping; that ID is outside the Rassvet chain and does not unlock its configured successors or medals.

Configure coins/items and UNIQUE/REPEAT afterward through **Modificar → Loot**. Existing prize identities and earned history are retained when changing the definition.

Skin selection synchronizes only references to resources already provided by the installed RCT dependency. A client renderer subclass preserves normal RCT rendering and changes only the texture for owned IDs. No RCT images are copied into the Zian RCT JAR. Importing arbitrary PNGs is not part of this version.

## Permissions and compatibility

The creator requires both `zianrct.admin.npc.edit` and **zianrct.admin.trainer.create**. Both creation and definition updates recheck these on the server using a single-use, player-bound DESIGN session.

Example for an existing admin group:

```text
lp group admin permission set zianrct.admin.trainer.create true
```

NPC spawn/move/delete and loot retain their previous separate nodes. An active battle against the ID or an in-flight resource reload prevents definition changes. Clients cannot provide arbitrary NPC NBT, file paths or executable commands.

Install **beta.9 on server and clients**; editor channel is now npc4. The creator save payload is bounded to 16 KiB. Medal/reward formats retain the previous compatibility rules.

## Tests and remaining scope

Automated coverage validates creator bounds, persisted definitions/skin references, isolated resource paths, dialogue perspectives, difficulty output and readable reward messages, alongside existing reward/cooldown/ownership tests. Dedicated CI includes an independent double-format trainer fixture and checks native resource verification.

Live checks: create a new NORMAL trainer with two valid Pokémon, wait for verification, spawn it, check name/skin/dialogues, fight while remaining in the current RCT series, confirm no additional Rassvet medal/cap change, set loot and repeat wait, restart and repeat. Client rendering and a real battle require operator validation.

Legendary encounters are a later stage. This creator supplies independent bosses and persistent victory/reward infrastructure, but does not currently unlock, spawn, deliver or guarantee capture of legendary Pokémon. That feature needs a separate player-bound encounter journal covering creation, ownership, capture, escape and disconnect/restart recovery.
