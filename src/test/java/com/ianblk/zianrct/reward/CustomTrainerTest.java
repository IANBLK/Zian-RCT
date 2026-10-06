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
    @Test void definitionsAndSkinReferencesSurviveRestart() throws Exception{
        CustomTrainerStore.boot(temp);CustomTrainerStore.save(trainer());CustomTrainerStore.clear();CustomTrainerStore.boot(temp);
        assertEquals(trainer(),CustomTrainerStore.get(trainer().id()));
        assertEquals(CustomTrainer.SKINS.get(1),CustomTrainerStore.skins().get(trainer().id()));
        assertTrue(Files.exists(temp.resolve("zianrct-custom-trainers.json")));
    }
    @Test void difficultyAppliesPerfectHardBossIvsAndNativeHeldItemFormat(){
        for(String difficulty:List.of("FACIL","NORMAL","DIFICIL","JEFE")){
            var original=trainer();var d=new CustomTrainer(original.id(),original.name(),difficulty,original.format(),original.skin(),original.team(),original.start(),original.playerWins(),original.playerLoses());
            var resources=CustomTrainerResources.build(Map.of(d.id(),d));
            var team=JsonParser.parseString(new String(resources.get("trainers/"+d.id()+".json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("team");
            int totalEv=0;
            for(var element:team){
                var member=element.getAsJsonObject();
                for(var stat:member.getAsJsonObject("ivs").entrySet())assertEquals(switch(difficulty){case "FACIL"->10;case "NORMAL"->20;default->31;},stat.getValue().getAsInt());
                totalEv=member.getAsJsonObject("evs").entrySet().stream().mapToInt(e->e.getValue().getAsInt()).sum();
                assertTrue(totalEv<=510);
                if(difficulty.equals("FACIL"))assertFalse(member.has("heldItem"));
                else assertEquals(switch(difficulty){case "NORMAL"->"oran_berry";case "DIFICIL"->"sitrus_berry";default->"leftovers";},member.getAsJsonArray("heldItem").get(0).getAsString());
            }
        }
    }
    @Test void everyNamedCatalogSkinIsABundled64PixelMinecraftTexture() throws Exception{
        assertEquals(6,CustomTrainer.SKINS.size());assertEquals(CustomTrainer.SKINS.size(),CustomTrainer.SKIN_NAMES.size());
        for(String path:CustomTrainer.SKINS){
            try(var stream=getClass().getResourceAsStream("/assets/zianrct/"+path.substring("zianrct:".length()))){
                assertNotNull(stream,path);var image=javax.imageio.ImageIO.read(stream);
                assertNotNull(image);assertEquals(64,image.getWidth());assertEquals(64,image.getHeight());
            }
        }
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
