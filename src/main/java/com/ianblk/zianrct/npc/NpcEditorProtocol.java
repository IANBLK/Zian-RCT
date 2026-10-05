package com.ianblk.zianrct.npc;
import java.util.Set;
import java.util.UUID;

public final class NpcEditorProtocol {
    public static final Set<String> ACTIONS = Set.of("list", "create", "search", "choose_template", "select_npc", "select_delete",
            "next", "previous", "spawn_prompt", "spawn", "persistent", "movement", "move", "delete_prompt", "delete", "cancel",
            "add_item", "clear_items", "money", "remove_reward");
    private NpcEditorProtocol() {}
    public static void action(String nonce, String action, String value, String extra) {
        UUID.fromString(nonce);
        if (!ACTIONS.contains(action) || value == null || value.length() > 128 || extra == null || extra.length() > 32)
            throw new IllegalArgumentException("Invalid editor action");
    }
    public static boolean allowed(NpcEditorState.Mode mode, String confirmation, String action) {
        if (Set.of("list","create").contains(action)) return true;
        return switch (mode) {
            case LIST -> Set.of("select_npc","select_delete","next","previous").contains(action);
            case CREATE -> Set.of("search","choose_template","next","previous","spawn_prompt","cancel").contains(action)
                    || (action.equals("spawn") && confirmation.equals("spawn"));
            case EDIT -> Set.of("persistent","movement","move","delete_prompt","cancel","add_item","clear_items","money","remove_reward").contains(action)
                    || (action.equals("delete") && confirmation.equals("delete"));
        };
    }
    public static void snapshot(String json) {
        if (json == null || json.length() > 16384) throw new IllegalArgumentException("Editor payload too large");
    }
}
