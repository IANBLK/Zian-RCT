package com.ianblk.zianrct.medal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public final class MedalStore {
    private static final int SCHEMA_VERSION = 2;
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final Path file;
    private MedalLedger ledger;

    private MedalStore(Path file, MedalLedger ledger) {
        this.file = file;
        this.ledger = ledger;
    }

    public static MedalStore open(Path file) throws IOException {
        if (Files.notExists(file)) {
            return new MedalStore(file, new MedalLedger());
        }

        try (Reader reader = Files.newBufferedReader(file)) {
            FileModel model = GSON.fromJson(reader, FileModel.class);
            if (model == null) {
                throw new IOException("El archivo de medallas está vacío: " + file);
            }
            if (model.schemaVersion() != 1 && model.schemaVersion() != SCHEMA_VERSION) {
                throw new IOException(
                        "Versión de persistencia de medallas no soportada: " + model.schemaVersion()
                );
            }

            LinkedHashMap<UUID, MedalLedger.PlayerSnapshot> snapshot = new LinkedHashMap<>();
            List<PlayerModel> players = model.players() == null ? List.of() : model.players();
            for (PlayerModel player : players) {
                if (player == null || player.uuid() == null || player.uuid().isBlank()) {
                    throw new IOException("Entrada de jugador inválida en " + file);
                }
                UUID uuid;
                try {
                    uuid = UUID.fromString(player.uuid());
                } catch (IllegalArgumentException exception) {
                    throw new IOException("UUID inválido en persistencia de medallas: " + player.uuid(), exception);
                }
                if (snapshot.containsKey(uuid)) {
                    throw new IOException("Jugador duplicado en persistencia de medallas: " + uuid);
                }
                List<MedalRecord> records = player.medals() == null
                        ? List.of()
                        : List.copyOf(player.medals());
                LinkedHashSet<String> revoked = new LinkedHashSet<>();
                if (model.schemaVersion() >= 2 && player.revoked() != null) {
                    revoked.addAll(player.revoked());
                }
                snapshot.put(uuid, new MedalLedger.PlayerSnapshot(records, revoked));
            }

            return new MedalStore(file, MedalLedger.fromSnapshot(snapshot));
        } catch (JsonParseException | IllegalArgumentException exception) {
            throw new IOException("Persistencia de medallas inválida: " + file, exception);
        }
    }

    public synchronized boolean grantIfAbsent(UUID playerId, MedalRecord record, boolean clearRevocation)
            throws IOException {
        MedalLedger candidate = ledger.copy();
        if (!candidate.grantIfAbsent(playerId, record, clearRevocation)) {
            return false;
        }
        write(candidate);
        ledger = candidate;
        return true;
    }

    public synchronized boolean revoke(UUID playerId, String medalId) throws IOException {
        MedalLedger candidate = ledger.copy();
        if (!candidate.revoke(playerId, medalId)) {
            return false;
        }
        write(candidate);
        ledger = candidate;
        return true;
    }

    public synchronized List<MedalRecord> medals(UUID playerId) {
        return ledger.medals(playerId);
    }

    public synchronized boolean has(UUID playerId, String medalId) {
        return ledger.find(playerId, medalId).isPresent();
    }

    public synchronized boolean isRevoked(UUID playerId, String medalId) {
        return ledger.isRevoked(playerId, medalId);
    }

    private void write(MedalLedger candidate) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<PlayerModel> players = new ArrayList<>();
        candidate.snapshot().forEach((uuid, snapshot) -> players.add(
                new PlayerModel(uuid.toString(), snapshot.medals(), new ArrayList<>(snapshot.revoked()))
        ));
        FileModel model = new FileModel(SCHEMA_VERSION, players);

        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary)) {
            GSON.toJson(model, writer);
        }

        try {
            Files.move(
                    temporary,
                    file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record FileModel(int schemaVersion, List<PlayerModel> players) {
    }

    private record PlayerModel(String uuid, List<MedalRecord> medals, List<String> revoked) {
    }
}
