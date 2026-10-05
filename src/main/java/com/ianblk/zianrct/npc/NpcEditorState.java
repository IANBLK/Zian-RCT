package com.ianblk.zianrct.npc;
import java.util.List;
import java.util.UUID;

public record NpcEditorState(String nonce, String trainer, String npc, boolean persistent, boolean frozen,
                             List<String> matches, List<String> rewards, String currency, long coins, String notice) {
    public NpcEditorState {
        UUID.fromString(nonce);
        if (trainer == null || trainer.length() > 128 || npc == null || npc.length() > 40 || currency == null
                || currency.length() > 128 || coins < 0 || coins > 1728 || notice == null || notice.length() > 256)
            throw new IllegalArgumentException("Editor snapshot invalid");
        matches = List.copyOf(matches); rewards = List.copyOf(rewards);
        if (matches.size() > 12 || matches.stream().anyMatch(s -> s.length() > 128)
                || rewards.size() > 9 || rewards.stream().anyMatch(s -> s.length() > 256))
            throw new IllegalArgumentException("Editor snapshot too large");
    }
}
