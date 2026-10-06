package com.ianblk.zianrct.npc;
import java.util.List;
import java.util.UUID;

public record NpcEditorState(String nonce, Mode mode, String trainer, String npc, boolean persistent, boolean frozen,
                             List<String> matches, List<NpcView> npcs, List<String> rewards, String currency,
                             long coins, String notice, String query, int page, int pages, String confirmation,
                             String rewardMode,long cooldownMinutes,String nextReward,String battleFormat) {
    public enum Mode { LIST, CREATE, EDIT }
    public record NpcView(String uuid, String trainer, int x, int y, int z, int distance) {
        public NpcView {
            UUID.fromString(uuid);
            if (trainer == null || trainer.length() > 128 || distance < 0) throw new IllegalArgumentException("Invalid NPC view");
        }
    }
    public NpcEditorState {
        UUID.fromString(nonce);
        if (mode == null || trainer == null || trainer.length() > 128 || npc == null || npc.length() > 40 || currency == null
                || currency.length() > 128 || coins < 0 || coins > 1728 || notice == null || notice.length() > 256
                || query == null || query.length() > 128 || page < 0 || pages < 1 || page >= pages
                || !java.util.Set.of("", "spawn", "delete").contains(confirmation)
                || !java.util.Set.of("UNIQUE","REPEAT").contains(rewardMode) || cooldownMinutes<0 || cooldownMinutes>43200
                || nextReward==null || nextReward.length()>256 || battleFormat==null || battleFormat.length()>64)
            throw new IllegalArgumentException("Editor snapshot invalid");
        if (!npc.isEmpty()) UUID.fromString(npc);
        matches = List.copyOf(matches); npcs = List.copyOf(npcs); rewards = List.copyOf(rewards);
        if (matches.size() > 5 || matches.stream().anyMatch(s -> s.length() > 128) || npcs.size() > 5
                || rewards.size() > 9 || rewards.stream().anyMatch(s -> s.length() > 256))
            throw new IllegalArgumentException("Editor snapshot too large");
    }
}
