package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.rct.RctTrainerOverrides;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TrainerOverrideTest {
    @Test void rematchesDoNotChangePrerequisitesCapsOrNativeBattleWait() {
        var source=JsonParser.parseString("{\"requiredDefeats\":[[\"previous\"]],\"relativeLevelCap\":-30,\"series\":[\"rassvet\"],\"battleCooldownTicks\":240,\"maxTrainerWins\":5,\"maxTrainerDefeats\":1}").getAsJsonObject();
        var result=JsonParser.parseString(RctTrainerOverrides.repeatMob(source.toString())).getAsJsonObject();
        assertEquals(-1,result.get("maxTrainerWins").getAsInt());assertEquals(-1,result.get("maxTrainerDefeats").getAsInt());
        result.remove("maxTrainerWins");result.remove("maxTrainerDefeats");source.remove("maxTrainerWins");source.remove("maxTrainerDefeats");
        assertEquals(source,result);
    }
    @Test void doublesPreservePokemonMovesLevelsAiAndRules() {
        var source=JsonParser.parseString("{\"name\":\"Boss\",\"team\":[{\"species\":\"pikachu\",\"level\":50,\"moveset\":[\"thunderbolt\"]}],\"ai\":{\"type\":\"rct\"},\"battleRules\":{\"maxItemUses\":3}}").getAsJsonObject();
        var result=JsonParser.parseString(RctTrainerOverrides.format(source.toString(),"GEN_9_DOUBLES")).getAsJsonObject();
        assertEquals("GEN_9_DOUBLES",result.remove("battleFormat").getAsString());assertEquals(source,result);
        assertThrows(IllegalArgumentException.class,()->RctTrainerOverrides.format(source.toString(),"arbitrary"));
    }
}
