package com.ianblk.zianrct.npc;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcEditorTest {
    @Test void sessionsArePlayerBoundExpireAndRejectReplay() {
        var sessions=new NpcEditorSessions();
        UUID alice=UUID.randomUUID(),bob=UUID.randomUUID(),npc=UUID.randomUUID();
        var first=sessions.open(alice,npc,"trainer",List.of("trainer"),1000);
        assertNull(sessions.take(bob,first.token(),1100));
        assertEquals(first,sessions.take(alice,first.token(),1100));
        assertNull(sessions.take(alice,first.token(),1700));
        var next=sessions.open(alice,npc,"trainer",List.of(),1200);
        assertNull(sessions.take(alice,next.token(),1300));
        assertEquals(next,sessions.take(alice,next.token(),1800));
        var expired=sessions.open(alice,npc,"trainer",List.of(),2400);
        assertNull(sessions.take(alice,expired.token(),302401));
    }
    @Test void reopeningInvalidatesOldSnapshotsAndLogoutClearsTokens() {
        var sessions=new NpcEditorSessions();UUID player=UUID.randomUUID();
        var old=sessions.open(player,null,"trainer",List.of(),0);
        var replacement=sessions.open(player,null,"trainer",List.of(),100);
        assertNull(sessions.take(player,old.token(),600));
        sessions.remove(player);
        assertNull(sessions.take(player,replacement.token(),600));
    }
    @Test void actionsAndSnapshotsRejectOversizedOrUnexpectedData() {
        String nonce=UUID.randomUUID().toString();
        assertThrows(IllegalArgumentException.class,()->NpcEditorProtocol.action(nonce,"execute","op attacker",""));
        assertThrows(IllegalArgumentException.class,()->NpcEditorProtocol.action(nonce,"money","x".repeat(129),"5"));
        assertThrows(IllegalArgumentException.class,()->NpcEditorProtocol.snapshot("x".repeat(16385)));
        assertThrows(IllegalArgumentException.class,()->new NpcEditorState(nonce,"trainer","",true,true,
                Collections.nCopies(13,"trainer"),List.of(),"",0,""));
        assertThrows(IllegalArgumentException.class,()->new NpcEditorState(nonce,"trainer","",true,true,
                List.of(),List.of(),"coin",1729,""));
    }
}
