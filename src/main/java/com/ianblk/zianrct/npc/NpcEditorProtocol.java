package com.ianblk.zianrct.npc;
import java.util.Set;
import java.util.UUID;

public final class NpcEditorProtocol {
    public static final Set<String> ACTIONS = Set.of("search", "select", "nearby", "spawn", "persistent", "movement",
            "add_item", "clear_items", "money", "remove_reward");
    private NpcEditorProtocol() {}
    public static void action(String nonce, String action, String value, String extra) {
        UUID.fromString(nonce);
        if (!ACTIONS.contains(action) || value == null || value.length() > 128 || extra == null || extra.length() > 32)
            throw new IllegalArgumentException("Invalid editor action");
    }
    public static void snapshot(String json) {
        if (json == null || json.length() > 16384) throw new IllegalArgumentException("Editor payload too large");
    }
}
