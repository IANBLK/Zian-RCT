package com.ianblk.zianrct.medal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class MedalLedger {
    private final LinkedHashMap<UUID, PlayerState> players;

    public MedalLedger() {
        this.players = new LinkedHashMap<>();
    }

    private MedalLedger(LinkedHashMap<UUID, PlayerState> players) {
        this.players = players;
    }

    public boolean grantIfAbsent(UUID playerId, MedalRecord record, boolean clearRevocation) {
        PlayerState state = players.computeIfAbsent(playerId, ignored -> new PlayerState());
        if (!clearRevocation && state.revoked.contains(record.medalId())) {
            return false;
        }
        if (state.medals.containsKey(record.medalId())) {
            return false;
        }
        if (clearRevocation) {
            state.revoked.remove(record.medalId());
        }
        state.medals.put(record.medalId(), record);
        return true;
    }

    public boolean revoke(UUID playerId, String medalId) {
        PlayerState state = players.computeIfAbsent(playerId, ignored -> new PlayerState());
        boolean removed = state.medals.remove(medalId) != null;
        boolean newlyRevoked = state.revoked.add(medalId);
        return removed || newlyRevoked;
    }

    public boolean isRevoked(UUID playerId, String medalId) {
        PlayerState state = players.get(playerId);
        return state != null && state.revoked.contains(medalId);
    }

    public Optional<MedalRecord> find(UUID playerId, String medalId) {
        PlayerState state = players.get(playerId);
        return state == null ? Optional.empty() : Optional.ofNullable(state.medals.get(medalId));
    }

    public List<MedalRecord> medals(UUID playerId) {
        PlayerState state = players.get(playerId);
        if (state == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(state.medals.values()));
    }

    public Map<UUID, PlayerSnapshot> snapshot() {
        LinkedHashMap<UUID, PlayerSnapshot> snapshot = new LinkedHashMap<>();
        players.forEach((uuid, state) -> snapshot.put(
                uuid,
                new PlayerSnapshot(
                        Collections.unmodifiableList(new ArrayList<>(state.medals.values())),
                        Collections.unmodifiableSet(new LinkedHashSet<>(state.revoked))
                )
        ));
        return Collections.unmodifiableMap(snapshot);
    }

    public MedalLedger copy() {
        LinkedHashMap<UUID, PlayerState> copy = new LinkedHashMap<>();
        players.forEach((uuid, state) -> copy.put(uuid, state.copy()));
        return new MedalLedger(copy);
    }

    public static MedalLedger fromSnapshot(Map<UUID, PlayerSnapshot> snapshot) {
        MedalLedger ledger = new MedalLedger();
        snapshot.forEach((uuid, player) -> {
            PlayerState state = ledger.players.computeIfAbsent(uuid, ignored -> new PlayerState());
            for (MedalRecord record : player.medals()) {
                if (state.medals.putIfAbsent(record.medalId(), record) != null) {
                    throw new IllegalArgumentException(
                            "Medalla duplicada en persistencia para " + uuid + ": " + record.medalId()
                    );
                }
            }
            for (String medalId : player.revoked()) {
                if (medalId == null || medalId.isBlank()) {
                    throw new IllegalArgumentException("Id de medalla revocada inválido para " + uuid);
                }
                if (state.medals.containsKey(medalId)) {
                    throw new IllegalArgumentException(
                            "La medalla no puede estar concedida y revocada a la vez para " + uuid + ": " + medalId
                    );
                }
                state.revoked.add(medalId);
            }
        });
        return ledger;
    }

    public record PlayerSnapshot(List<MedalRecord> medals, Set<String> revoked) {
        public PlayerSnapshot {
            medals = medals == null ? List.of() : List.copyOf(medals);
            revoked = revoked == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(revoked));
        }
    }

    private static final class PlayerState {
        private final LinkedHashMap<String, MedalRecord> medals = new LinkedHashMap<>();
        private final LinkedHashSet<String> revoked = new LinkedHashSet<>();

        private PlayerState copy() {
            PlayerState copy = new PlayerState();
            copy.medals.putAll(medals);
            copy.revoked.addAll(revoked);
            return copy;
        }
    }
}
