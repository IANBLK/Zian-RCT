package com.ianblk.zianrct.reward;

import java.util.List;
import java.util.UUID;

public record RewardClaim(UUID id, UUID player, String trainer, long created, List<Part> parts,
                          long cycle, long cooldownMinutes, UUID battle) {
    public RewardClaim(UUID id,UUID player,String trainer,long created,List<Part> parts){this(id,player,trainer,created,parts,0,0,null);}
    public long nextEligibleAt(){return Math.addExact(created,Math.multiplyExact(cooldownMinutes,60000L));}
    public RewardClaim {
        if((cycle>0 && cooldownMinutes==0) || (cooldownMinutes>0 && battle==null))
            throw new IllegalArgumentException("Ciclo repetible sin evidencia de combate");
        if (id == null || player == null || created < 0 || cycle < 0 || cooldownMinutes < 0 || cooldownMinutes > 43200) throw new IllegalArgumentException("Reclamación inválida");
        TrainerRewardConfig.validId(trainer);
        parts = List.copyOf(parts);
        if (parts.isEmpty() || parts.size() > 9) throw new IllegalArgumentException("Componentes inválidos");
    }
    public boolean complete() { return parts.stream().allMatch(p -> p.phase() == Phase.DELIVERED); }
    public boolean review() { return parts.stream().anyMatch(p -> p.phase() == Phase.APPLYING || p.phase() == Phase.REVIEW_REQUIRED); }
    public enum Phase { PENDING, APPLYING, DELIVERED, REVIEW_REQUIRED }
    public enum Kind { ITEM, COINS }
    public record Part(Kind kind, String data, long amount, Phase phase, String detail) {
        public Part {
            if (kind == null || data == null || data.isBlank() || data.length() > 65536 || phase == null
                    || detail == null || detail.length() > 512) throw new IllegalArgumentException("Componente inválido");
            if (kind == Kind.COINS && (amount < 1 || amount > 1728 || !data.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))
                throw new IllegalArgumentException("Crédito inválido");
            if (kind == Kind.ITEM && amount != 1) throw new IllegalArgumentException("Objeto inválido");
        }
        public Part phase(Phase phase, String detail) { return new Part(kind, data, amount, phase, detail); }
    }
}
