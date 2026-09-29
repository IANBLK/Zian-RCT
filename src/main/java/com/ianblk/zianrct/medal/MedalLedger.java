package com.ianblk.zianrct.medal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MedalLedger {
    private final LinkedHashMap<UUID, LinkedHashMap<String, MedalRecord>> players;

    public MedalLedger() {
        this.players = new LinkedHashMap<>();
    }

    private MedalLedger(LinkedHashMap<UUID, LinkedHashMap<String, MedalRecord>> players) {
        this.players = players;
    }

    public boolean grantIfAbsent(UUID playerId, MedalRecord record) {
        LinkedHashMap<String, MedalRecord> medals = players.computeIfAbsent(
                playerId,
                ignored -> new LinkedHashMap<>()
        );
        if (medals.containsKey(record.medalId())) {
            return false;
        }
        medals.put(record.medalId(), record);
        return true;
    }

    public boolean revoke(UUID playerId, String medalId) {
        LinkedHashMap<String, MedalRecord> medals = players.get(playerId);
        if (medals == null || medals.remove(medalId) == null) {
            return false;
        }
        if (medals.isEmpty()) {
            players.remove(playerId);
        }
        return true;
    }

    public Optional<MedalRecord> find(UUID playerId, String medalId) {
        LinkedHashMap<String, MedalRecord> medals = players.get(playerId);
        return medals == null ? Optional.empty() : Optional.ofNullable(medals.get(medalId));
    }

    public List<MedalRecord> medals(UUID playerId) {
        LinkedHashMap<String, MedalRecord> medals = players.get(playerId);
        if (medals == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(medals.values()));
    }

    public Map<UUID, List<MedalRecord>> snapshot() {
        LinkedHashMap<UUID, List<MedalRecord>> snapshot = new LinkedHashMap<>();
        players.forEach((uuid, medals) -> snapshot.put(
                uuid,
                Collections.unmodifiableList(new ArrayList<>(medals.values()))
        ));
        return Collections.unmodifiableMap(snapshot);
    }

    public MedalLedger copy() {
        LinkedHashMap<UUID, LinkedHashMap<String, MedalRecord>> copy = new LinkedHashMap<>();
        players.forEach((uuid, medals) -> copy.put(uuid, new LinkedHashMap<>(medals)));
        return new MedalLedger(copy);
    }

    public static MedalLedger fromSnapshot(Map<UUID, List<MedalRecord>> snapshot) {
        MedalLedger ledger = new MedalLedger();
        snapshot.forEach((uuid, records) -> records.forEach(record -> {
            if (!ledger.grantIfAbsent(uuid, record)) {
                throw new IllegalArgumentException(
                        "Medalla duplicada en persistencia para " + uuid + ": " + record.medalId()
                );
            }
        }));
        return ledger;
    }
}
