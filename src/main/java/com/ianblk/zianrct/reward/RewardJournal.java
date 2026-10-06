package com.ianblk.zianrct.reward;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** A durable intent precedes every external component. Interrupted intents are never retried. */
public final class RewardJournal {
    private final Path path;
    private final Map<UUID, RewardClaim> claims = new LinkedHashMap<>();
    private final Map<String,RewardClaim> latest = new HashMap<>();
    private boolean healthy = true;
    private RewardJournal(Path path) { this.path = path; }
    public static RewardJournal open(Path path) throws IOException {
        RewardJournal journal = new RewardJournal(path);
        if (!Files.exists(path)) return journal;
        try {
            var json = RewardFiles.read(path, 16777216).getAsJsonObject();
            int version=json.get("schemaVersion").getAsBigDecimal().intValueExact();
            if (!json.keySet().equals(Set.of("schemaVersion", "claims")) || (version!=1 && version!=2))
                throw new IllegalArgumentException("Versión de registro no soportada");
            Model model = RewardFiles.GSON.fromJson(json, Model.class);
            if (model.claims() == null || model.claims().size() > 20000) throw new IllegalArgumentException("Registro inválido");
            Set<String> unique = new HashSet<>();
            Set<String> battles = new HashSet<>();
            Map<String,Long> counts = new HashMap<>();
            for (RewardClaim claim : model.claims()) {
                if (claim == null || !claim.id().equals(id(claim.player(), claim.trainer(),claim.cycle()))
                        || (version==1 && (claim.cycle()!=0 || claim.cooldownMinutes()!=0 || claim.battle()!=null))
                        || !unique.add(claim.player() + ":" + claim.trainer()+":"+claim.cycle())
                        || journal.claims.putIfAbsent(claim.id(), claim) != null) throw new IllegalArgumentException("Reclamación duplicada o inválida");
                if(claim.battle()!=null && !battles.add(key(claim.player(),claim.trainer())+":"+claim.battle()))throw new IllegalArgumentException("Combate repetido en el registro");
                counts.merge(key(claim.player(),claim.trainer()),1L,Long::sum);
            }
            journal.reindex();
            for(var claim:journal.claims.values())
                if(claim.cycle()<journal.latest(claim.player(),claim.trainer()).cycle() && !claim.complete())
                    throw new IllegalArgumentException("Ciclo anterior sin resolver");
            for(var entry:journal.latest.entrySet()){
                long count=counts.get(entry.getKey());
                if(count!=entry.getValue().cycle()+1)throw new IllegalArgumentException("Historial de ciclos incompleto");
            }
            return journal;
        } catch (RuntimeException error) { throw new IOException("Registro de recompensas inválido; conserva el archivo", error); }
    }
    public static UUID id(UUID player, String trainer) {
        return id(player,trainer,0);
    }
    public static UUID id(UUID player,String trainer,long cycle){
        return UUID.nameUUIDFromBytes(("zianrct:trainer_reward:"+player+":"+trainer+(cycle==0?"":":cycle:"+cycle)).getBytes(StandardCharsets.UTF_8));
    }
    private static String key(UUID player,String trainer){return player+":"+trainer;}
    private void reindex(){
        latest.clear();
        for(var claim:claims.values())latest.merge(key(claim.player(),claim.trainer()),claim,(a,b)->a.cycle()>b.cycle()?a:b);
    }
    public synchronized RewardClaim latest(UUID player,String trainer){return latest.get(key(player,trainer));}
    public synchronized long remaining(UUID player,String trainer,RewardDefinition definition,long now){
        var last=latest(player,trainer);
        if(last==null)return 0;
        if(!last.complete())return -2;
        if(definition.mode()==RewardDefinition.Mode.UNIQUE)return -1;
        return Math.max(0,last.nextEligibleAt()-now);
    }
    public synchronized boolean reserve(UUID player, String trainer, RewardDefinition reward) throws IOException {
        return reserveAt(player,trainer,reward,System.currentTimeMillis(),null);
    }
    public synchronized boolean reserveAt(UUID player,String trainer,RewardDefinition reward,long now,UUID battle) throws IOException {
        ensureHealthy();
        var last=latest(player,trainer);
        if(remaining(player,trainer,reward,now)!=0)return false;
        if(reward.mode()==RewardDefinition.Mode.REPEAT && battle==null)throw new IllegalArgumentException("Falta el identificador del combate");
        if(battle!=null && claims.values().stream().anyMatch(c -> c.player().equals(player) && c.trainer().equals(trainer) && battle.equals(c.battle())))return false;
        long cycle=last==null?0:Math.addExact(last.cycle(),1);
        UUID id=id(player,trainer,cycle);
        if (reward.empty()) return false;
        List<RewardClaim.Part> parts = new ArrayList<>();
        for (String item : reward.items()) parts.add(new RewardClaim.Part(RewardClaim.Kind.ITEM, item, 1, RewardClaim.Phase.PENDING, ""));
        if (reward.coins() > 0) parts.add(new RewardClaim.Part(RewardClaim.Kind.COINS, reward.currency(), reward.coins(), RewardClaim.Phase.PENDING, ""));
        Map<UUID, RewardClaim> candidate = new LinkedHashMap<>(claims);
        candidate.put(id, new RewardClaim(id, player, trainer, now, parts,cycle,reward.cooldownMinutes(),battle));
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
        candidate.put(id, new RewardClaim(id, original.player(), original.trainer(), original.created(), parts,original.cycle(),original.cooldownMinutes(),original.battle()));
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
            String encoded = RewardFiles.GSON.toJson(new Model(2, List.copyOf(candidate.values())));
            if (encoded.getBytes(StandardCharsets.UTF_8).length > 16777216) throw new IOException("Registro demasiado grande");
            RewardFiles.write(path, new Model(2, List.copyOf(candidate.values())));
            claims.clear(); claims.putAll(candidate);
            reindex();
        } catch (IOException error) { healthy = false; throw error; }
    }
    private record Model(int schemaVersion, List<RewardClaim> claims) {}
}
