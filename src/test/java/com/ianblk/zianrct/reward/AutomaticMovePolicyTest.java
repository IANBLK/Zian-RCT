package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.creator.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AutomaticMovePolicyTest {
    private AutomaticMovePolicy.Candidate move(String id,String type,double power,boolean status,boolean stab){return new AutomaticMovePolicy.Candidate(id,type,power,100,status,stab);}
    @Test void difficultyRanksWeakAndStrongAttacksDifferently(){
        var pool=List.of(move("tackle","normal",40,false,false),move("bodyslam","normal",85,false,false),move("hypervoice","normal",90,false,false),move("swift","normal",60,false,false),move("quickattack","normal",40,false,false));
        assertFalse(AutomaticMovePolicy.select("FACIL",pool).contains("hypervoice"));
        assertTrue(AutomaticMovePolicy.select("NORMAL",pool).contains("hypervoice"));
    }
    @Test void hardPreservesCoverageAndReservesOneSupportMove(){
        var pool=List.of(move("thunderbolt","electric",90,false,true),move("thunder","electric",110,false,true),move("surf","water",90,false,false),move("icebeam","ice",90,false,false),move("protect","normal",0,true,false),move("growl","normal",0,true,false));
        var selected=AutomaticMovePolicy.select("JEFE",pool);
        assertEquals(4,selected.size());assertTrue(selected.containsAll(List.of("surf","icebeam","protect")));
        assertFalse(selected.contains("growl"));
    }
    @Test void selectionIsStableUniqueAndNeverInventsMoves(){
        var pool=new ArrayList<>(List.of(move("tackle","normal",40,false,false),move("splash","normal",0,true,false),move("tackle","normal",40,false,false)));
        for(String difficulty:List.of("FACIL","NORMAL","DIFICIL","JEFE")){
            var result=AutomaticMovePolicy.select(difficulty,pool);Collections.reverse(pool);
            assertEquals(result,AutomaticMovePolicy.select(difficulty,pool));assertEquals(2,result.size());assertTrue(result.contains("tackle"));
        }
        assertTrue(AutomaticMovePolicy.select("NORMAL",List.of()).isEmpty());
    }
    @Test void oldDefinitionsRemainManualAndModeSurvivesFormatChange(){
        String old="{\"id\":\"zian_custom_old\",\"name\":\"Old\",\"difficulty\":\"NORMAL\",\"format\":\"GEN_9_SINGLES\",\"skin\":0,\"team\":[{\"species\":\"pikachu\",\"level\":25,\"moves\":[\"thunderbolt\"]}],\"start\":\"\",\"playerWins\":\"\",\"playerLoses\":\"\"}";
        var legacy=CustomTrainerStore.decode(old);assertFalse(legacy.autoMoves());assertEquals(List.of("thunderbolt"),legacy.team().getFirst().moves());
        var auto=new CustomTrainer(legacy.id(),legacy.name(),legacy.difficulty(),legacy.format(),legacy.skin(),legacy.team(),legacy.start(),legacy.playerWins(),legacy.playerLoses(),true);
        assertTrue(auto.format("GEN_9_SINGLES").autoMoves());assertTrue(CustomTrainerStore.decode(new com.google.gson.Gson().toJson(auto)).autoMoves());
    }
}
