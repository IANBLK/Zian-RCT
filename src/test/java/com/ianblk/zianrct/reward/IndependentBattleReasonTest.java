package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.creator.IndependentBattleReason;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndependentBattleReasonTest {
    @Test void independentRefusalsReportTheirRealReasonInsteadOfLeagueMembership(){
        assertEquals("trainer_busy",IndependentBattleReason.context(true,false,0,1,false,true));
        assertEquals("player_busy",IndependentBattleReason.context(false,true,0,1,false,true));
        assertEquals("on_cooldown",IndependentBattleReason.context(false,false,240,1,false,true));
        assertEquals("missing_pokemon",IndependentBattleReason.context(false,false,0,0,false,true));
        assertEquals("over_level_cap",IndependentBattleReason.context(false,false,0,1,true,true));
        assertEquals("done_generic",IndependentBattleReason.context(false,false,0,1,false,false));
        assertEquals("unknown_reason",IndependentBattleReason.context(false,false,0,1,false,true));
    }
}
