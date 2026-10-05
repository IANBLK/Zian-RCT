package com.ianblk.zianrct.npc;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcEditorTest {
    private NpcEditorSessions.Session open(NpcEditorSessions sessions,UUID player,UUID npc,long now){
        return sessions.open(player,npc,"trainer","",NpcEditorState.Mode.EDIT,"",0,Set.of(),now);
    }
    @Test void sessionsArePlayerBoundExpireAndRejectReplay(){
        var sessions=new NpcEditorSessions();UUID alice=UUID.randomUUID(),bob=UUID.randomUUID(),npc=UUID.randomUUID();
        var first=open(sessions,alice,npc,1000);
        assertNull(sessions.take(bob,first.token(),1100));
        assertEquals(first,sessions.take(alice,first.token(),1100));
        assertNull(sessions.take(alice,first.token(),1700));
        var next=open(sessions,alice,npc,1200);
        assertNull(sessions.take(alice,next.token(),1300));
        assertEquals(next,sessions.take(alice,next.token(),1800));
        var expired=open(sessions,alice,npc,2400);
        assertNull(sessions.take(alice,expired.token(),302401));
    }
    @Test void reopeningInvalidatesOldSnapshotsAndLogoutClearsTokens(){
        var sessions=new NpcEditorSessions();UUID player=UUID.randomUUID();
        var old=open(sessions,player,null,0);var replacement=open(sessions,player,null,100);
        assertNull(sessions.take(player,old.token(),600));sessions.remove(player);
        assertNull(sessions.take(player,replacement.token(),600));
    }
    @Test void actionsAndSnapshotsRejectOversizedOrUnexpectedData(){
        String nonce=UUID.randomUUID().toString();
        assertThrows(IllegalArgumentException.class,()->NpcEditorProtocol.action(nonce,"execute","op attacker",""));
        assertThrows(IllegalArgumentException.class,()->NpcEditorProtocol.action(nonce,"money","x".repeat(129),"5"));
        assertThrows(IllegalArgumentException.class,()->NpcEditorProtocol.snapshot("x".repeat(16385)));
        assertThrows(IllegalArgumentException.class,()->new NpcEditorState(nonce,NpcEditorState.Mode.CREATE,"","",false,false,
                Collections.nCopies(6,"trainer"),List.of(),List.of(),"",0,"","",0,1,""));
        assertThrows(IllegalArgumentException.class,()->new NpcEditorState(nonce,NpcEditorState.Mode.EDIT,"trainer","",true,true,
                List.of(),List.of(),List.of(),"coin",1729,"","",0,1,""));
    }
    @Test void creatingAndDeletingRequireTheirOwnServerConfirmedFlow(){
        assertFalse(NpcEditorProtocol.allowed(NpcEditorState.Mode.LIST,"","spawn"));
        assertFalse(NpcEditorProtocol.allowed(NpcEditorState.Mode.EDIT,"","spawn"));
        assertFalse(NpcEditorProtocol.allowed(NpcEditorState.Mode.CREATE,"","spawn"));
        assertTrue(NpcEditorProtocol.allowed(NpcEditorState.Mode.CREATE,"spawn","spawn"));
        assertFalse(NpcEditorProtocol.allowed(NpcEditorState.Mode.LIST,"delete","delete"));
        assertFalse(NpcEditorProtocol.allowed(NpcEditorState.Mode.EDIT,"","delete"));
        assertTrue(NpcEditorProtocol.allowed(NpcEditorState.Mode.EDIT,"delete","delete"));
        assertFalse(NpcEditorProtocol.allowed(NpcEditorState.Mode.CREATE,"","move"));
        assertTrue(NpcEditorProtocol.allowed(NpcEditorState.Mode.EDIT,"","move"));
    }
    @Test void onlyNpcIdsFromTheDisplayedPageAreAuthorized(){
        var sessions=new NpcEditorSessions();UUID player=UUID.randomUUID(),npc=UUID.randomUUID();
        var session=sessions.open(player,null,"","",NpcEditorState.Mode.LIST,"",0,Set.of(npc),0);
        assertTrue(session.visibleNpcs().contains(npc));assertFalse(session.visibleNpcs().contains(UUID.randomUUID()));
        assertThrows(UnsupportedOperationException.class,()->session.visibleNpcs().clear());
    }
    @Test void movingUsesTheAdministratorsBlockCentreFeetAndFacingIncludingNegativeCoordinates(){
        var at=NpcPlacement.at(383.1,64.5,-777.8,123.0f);
        assertEquals(383.5,at.x());assertEquals(64.5,at.y());assertEquals(-777.5,at.z());assertEquals(123.0f,at.yaw());
        assertThrows(IllegalArgumentException.class,()->NpcPlacement.at(Double.NaN,64,1,0));
        assertThrows(IllegalArgumentException.class,()->NpcPlacement.at(1,64,1,Float.POSITIVE_INFINITY));
    }
}
