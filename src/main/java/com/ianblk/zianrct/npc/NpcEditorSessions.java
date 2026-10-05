package com.ianblk.zianrct.npc;
import java.util.*;

public final class NpcEditorSessions {
    public record Session(UUID token, UUID npc, String trainer, List<String> matches, long expires) {}
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> last = new HashMap<>();
    public Session open(UUID player, UUID npc, String trainer, List<String> matches, long now) {
        Session session = new Session(UUID.randomUUID(), npc, trainer, List.copyOf(matches), now + 300000);
        sessions.put(player, session); return session;
    }
    public Session take(UUID player, UUID token, long now) {
        Session session = sessions.get(player);
        if (session == null || !session.token().equals(token) || now > session.expires()) return null;
        Long before = last.get(player);
        if (before != null && now - before < 500) return null;
        last.put(player, now); sessions.remove(player); return session;
    }
    public void remove(UUID player) { sessions.remove(player); last.remove(player); }
    public void clear() { sessions.clear(); last.clear(); }
}
