package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.creator.*;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomTrainerTest {
    @TempDir Path temp;
    @org.junit.jupiter.api.AfterEach void cleanup() throws Exception{CustomTrainerStore.clear();RewardTestFiles.cleanup(temp);}
    private CustomTrainer trainer(){
        return new CustomTrainer("zian_custom_reto","Guardia","JEFE","GEN_9_DOUBLES",1,
                List.of(new CustomTrainer.Member("pikachu",50,List.of("thunderbolt")),new CustomTrainer.Member("eevee",50,List.of("tackle"))),
                "Hola","Me has derrotado","Inténtalo de nuevo");
    }
    @Test void independentResourcesNeverUseRassvetOrRequireItsProgress(){
        var d=trainer();var resources=CustomTrainerResources.build(Map.of(d.id(),d));
        var mob=JsonParser.parseString(new String(resources.get("mobs/trainers/single/"+d.id()+".json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(List.of("zian_challenges"),new com.google.gson.Gson().fromJson(mob.get("series"),java.util.List.class));
        assertTrue(mob.get("optional").getAsBoolean());assertTrue(mob.getAsJsonArray("requiredDefeats").isEmpty());
        assertTrue(mob.getAsJsonArray("requiredSeries").isEmpty());assertEquals(-50,mob.get("relativeLevelCap").getAsInt());
        assertEquals(0,mob.get("spawnWeightFactor").getAsInt());assertTrue(mob.getAsJsonObject("winCommands").isEmpty());
        assertFalse(resources.keySet().stream().anyMatch(p->p.contains("rassvet")));
    }
    @Test void teamPresetAndDialogGenerationRespectTheChosenDefinition(){
        var d=trainer();var r=CustomTrainerResources.build(Map.of(d.id(),d));
        var team=JsonParser.parseString(new String(r.get("trainers/"+d.id()+".json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("GEN_9_DOUBLES",team.get("battleFormat").getAsString());assertEquals("Guardia",team.getAsJsonObject("name").get("literal").getAsString());
        var p=team.getAsJsonArray("team").get(0).getAsJsonObject();
        assertEquals(50,p.get("level").getAsInt());assertEquals(31,p.getAsJsonObject("ivs").get("hp").getAsInt());assertEquals(84,p.getAsJsonObject("evs").get("hp").getAsInt());
        var dialogs=JsonParser.parseString(new String(r.get("dialogs/trainers/single/"+d.id()+".json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("Me has derrotado",dialogs.getAsJsonArray("on_battle_lost").get(0).getAsJsonObject().get("literal").getAsString());
    }
    @Test void definitionsAndSkinReferencesSurviveRestartWithoutCopyingAssets() throws Exception{
        CustomTrainerStore.boot(temp);CustomTrainerStore.save(trainer());CustomTrainerStore.clear();CustomTrainerStore.boot(temp);
        assertEquals(trainer(),CustomTrainerStore.get(trainer().id()));
        assertEquals(CustomTrainer.SKINS.get(1),CustomTrainerStore.skins().get(trainer().id()));
        assertTrue(Files.exists(temp.resolve("zianrct-custom-trainers.json")));
    }
    @Test void arbitraryIdsBadLevelsAndIncompleteDoubleTeamsAreRejected(){
        var d=trainer();
        assertThrows(IllegalArgumentException.class,()->new CustomTrainer("rassvet_leader_novato",d.name(),d.difficulty(),d.format(),d.skin(),d.team(),d.start(),d.playerWins(),d.playerLoses()));
        assertThrows(IllegalArgumentException.class,()->new CustomTrainer.Member("pikachu",101,List.of("tackle")));
        assertThrows(IllegalArgumentException.class,()->new CustomTrainer.Member("../escape",25,List.of("tackle")));
        assertThrows(IllegalArgumentException.class,()->new CustomTrainer(d.id(),d.name(),d.difficulty(),d.format(),d.skin(),List.of(d.team().getFirst()),d.start(),d.playerWins(),d.playerLoses()));
    }
    @Test void malformedExistingDefinitionsArePreservedInsteadOfReset() throws Exception{
        var path=temp.resolve("zianrct-custom-trainers.json");Files.writeString(path,"{}");
        assertThrows(java.io.IOException.class,()->CustomTrainerStore.boot(temp));assertEquals("{}",Files.readString(path));
    }
    @Test void messagesUseReadableNamesAndCorrectSingularPluralDurations(){
        assertEquals("3 horas",RewardMessages.waitFor(10800000));
        assertEquals("1 minuto",RewardMessages.waitFor(60000));
        assertEquals("33 segundos",RewardMessages.waitFor(33000));
        assertEquals("Recompensa repetible del entrenador Novato entregada. Vuelve dentro de 3 horas para recibir otra recompensa.",
                RewardMessages.delivered("Novato",10800000,true));
        assertEquals("Recompensa del entrenador Guardia entregada.",RewardMessages.delivered("Guardia",0,false));
    }
}
