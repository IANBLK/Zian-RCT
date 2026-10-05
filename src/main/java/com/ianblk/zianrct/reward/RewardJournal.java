package com.ianblk.zianrct.reward;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** A durable intent precedes every external component. Interrupted intents are never retried. */
public final class RewardJournal {
    private final Path path;
    private final Map<UUID, RewardClaim> claims = new LinkedHashMap<>();
    private boolean healthy = true;
    private RewardJournal(Path path) { this.path = path; }
    public static RewardJournal open(Path path) throws IOException {
        RewardJournal journal = new RewardJournal(path);
        if (!Files.exists(path)) return journal;
        try {
            var json = RewardFiles.read(path, 16777216).getAsJsonObject();
            if (!json.keySet().equals(Set.of("schemaVersion", "claims")) || json.get("schemaVersion").getAsBigDecimal().intValueExact() != 1)
                throw new IllegalArgumentException("Versión de registro no soportada");
            Model model = RewardFiles.GSON.fromJson(json, Model.class);
            if (model.claims() == null || model.claims().size() > 20000) throw new IllegalArgumentException("Registro inválido");
            Set<String> unique = new HashSet<>();
            for (RewardClaim claim : model.claims()) {
                if (claim == null || !claim.id().equals(id(claim.player(), claim.trainer()))
                        || !unique.add(claim.player() + ":" + claim.trainer())
                        || journal.claims.putIfAbsent(claim.id(), claim) != null) throw new IllegalArgumentException("Reclamación duplicada o inválida");
            }
            return journal;
        } catch (RuntimeException error) { throw new IOException("Registro de recompensas inválido; conserva el archivo", error); }
    }
    public static UUID id(UUID player, String trainer) {
        return UUID.nameUUIDFromBytes(("zianrct:trainer_reward:" + player + ":" + trainer).getBytes(StandardCharsets.UTF_8));
    }
    public synchronized boolean reserve(UUID player, String trainer, RewardDefinition reward) throws IOException {
        ensureHealthy();
        UUID id = id(player, trainer);
        if (claims.containsKey(id)) return false;
        if (reward.empty()) return false;
        List<RewardClaim.Part> parts = new ArrayList<>();
        for (String item : reward.items()) parts.add(new RewardClaim.Part(RewardClaim.Kind.ITEM, item, 1, RewardClaim.Phase.PENDING, ""));
        if (reward.coins() > 0) parts.add(new RewardClaim.Part(RewardClaim.Kind.COINS, reward.currency(), reward.coins(), RewardClaim.Phase.PENDING, ""));
        Map<UUID, RewardClaim> candidate = new LinkedHashMap<>(claims);
        candidate.put(id, new RewardClaim(id, player, trainer, System.currentTimeMillis(), parts));
        persist(candidate);
        return true;
    }
    public synchronized RewardClaim get(UUID id) { return claims.get(id); }
    public synchronized List<RewardClaim> forPlayer(UUID player) {
        return claims.values().stream().filter(c -> c.player().equals(player)).toList();
    }
    public synchronized void phase(UUID id, int index, RewardClaim.Phase phase, String detail) throws IOException {
        ensureHealthy();
        RewardClaim original = Objects.requireNonNull(claims.get(id), "Reclamación inexistente");
        List<RewardClaim.Part> parts = new ArrayList<>(original.parts());
        var before = parts.get(index).phase();
        if (before == phase && parts.get(index).detail().equals(detail)) return;
        boolean allowed = switch (before) {
            case PENDING -> phase == RewardClaim.Phase.PENDING || phase == RewardClaim.Phase.APPLYING;
            case APPLYING -> phase == RewardClaim.Phase.PENDING || phase == RewardClaim.Phase.DELIVERED || phase == RewardClaim.Phase.REVIEW_REQUIRED;
            case REVIEW_REQUIRED -> phase == RewardClaim.Phase.DELIVERED;
            case DELIVERED -> false;
        };
        if (!allowed) throw new IllegalArgumentException("Transición de recompensa no permitida");
        parts.set(index, parts.get(index).phase(phase, detail));
        Map<UUID, RewardClaim> candidate = new LinkedHashMap<>(claims);
        candidate.put(id, new RewardClaim(id, original.player(), original.trainer(), original.created(), parts));
        persist(candidate);
    }
    public synchronized void resolve(UUID player, UUID id, int index, UUID admin, String decision, String evidence) throws IOException {
        ensureHealthy();
        if (!Set.of("confirmed_delivered", "confirmed_compensated").contains(decision)
                || evidence == null || evidence.isBlank() || evidence.length() > 160
                || evidence.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Decisión o evidencia inválida (máximo 160 caracteres)");
        RewardClaim claim = Objects.requireNonNull(claims.get(id), "Reclamación inexistente");
        if (!claim.player().equals(player)) throw new IllegalArgumentException("Jugador incorrecto");
        var phase = claim.parts().get(index).phase();
        if (phase != RewardClaim.Phase.APPLYING && phase != RewardClaim.Phase.REVIEW_REQUIRED)
            throw new IllegalArgumentException("Solo se resuelven componentes inciertos");
        phase(id, index, RewardClaim.Phase.DELIVERED, decision + " admin=" + admin + " evidence=" + evidence);
    }
    synchronized void ensureHealthy() throws IOException { if (!healthy) throw new IOException("Registro bloqueado por error de escritura; reinicia después de revisar"); }
    private void persist(Map<UUID, RewardClaim> candidate) throws IOException {
        if (candidate.size() > 20000) throw new IOException("Registro lleno; no se borran premios antiguos automáticamente");
        try {
            String encoded = RewardFiles.GSON.toJson(new Model(1, List.copyOf(candidate.values())));
            if (encoded.getBytes(StandardCharsets.UTF_8).length > 16777216) throw new IOException("Registro demasiado grande");
            RewardFiles.write(path, new Model(1, List.copyOf(candidate.values())));
            claims.clear(); claims.putAll(candidate);
        } catch (IOException error) { healthy = false; throw error; }
    }
    private record Model(int schemaVersion, List<RewardClaim> claims) {}
}
