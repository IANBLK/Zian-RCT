package com.ianblk.zianrct.client;
import com.google.gson.Gson;
import com.ianblk.zianrct.npc.NpcEditorState;

public final class ClientNpcEditorState {
    private static NpcEditorState pending;
    private ClientNpcEditorState() {}
    public static void accept(String json) { pending = new Gson().fromJson(json, NpcEditorState.class); }
    public static NpcEditorState consume() { var state = pending; pending = null; return state; }
    public static void clear() { pending = null; }
}
