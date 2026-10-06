package com.ianblk.zianrct.creator;
import com.google.gson.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public final class CustomTrainerResources {
    private CustomTrainerResources() {}
    public static Map<String,byte[]> build(Map<String,CustomTrainer> definitions){
        Map<String,byte[]> out=new LinkedHashMap<>();
        if(definitions.isEmpty())return out;
        var series=new JsonObject();series.add("title",literal("Retos independientes Zian"));series.addProperty("difficulty",0);
        series.addProperty("initialLevelCap",1);put(out,"series/zian_challenges.json",series);
        for(var d:definitions.values()){
            var team=new JsonObject();team.add("name",literal(d.name()));team.addProperty("battleFormat",d.format());
            var ai=new JsonObject();ai.addProperty("type","rct");var settings=new JsonObject();
            int tier=List.of("FACIL","NORMAL","DIFICIL","JEFE").indexOf(d.difficulty());
            settings.addProperty("maxSelectMargin",new double[]{0.6,0.25,0.1,0.01}[tier]);settings.addProperty("switchBias",new double[]{0.3,0.5,0.8,1}[tier]);
            ai.add("data",settings);team.add("ai",ai);
            var members=new JsonArray();
            for(var m:d.team()){
                var p=new JsonObject();p.addProperty("species",m.species());p.addProperty("level",m.level());
                p.add("moveset",new Gson().toJsonTree(m.moves()));
                var iv=new JsonObject();var ev=new JsonObject();
                for(String stat:List.of("hp","atk","def","spa","spd","spe")){
                    iv.addProperty(stat,new int[]{10,20,31,31}[tier]);ev.addProperty(stat,new int[]{0,0,64,84}[tier]);
                }
                p.add("ivs",iv);p.add("evs",ev);
                // Conservative universal items: no choice lock, recoil or battle gimmick dependencies.
                if(tier>0){var item=new JsonArray();item.add(new String[]{"","oran_berry","sitrus_berry","leftovers"}[tier]);p.add("heldItem",item);}
                members.add(p);
            }
            team.add("team",members);put(out,"trainers/"+d.id()+".json",team);
            var mob=new JsonObject();mob.add("series",new Gson().toJsonTree(List.of("zian_challenges")));
            mob.addProperty("optional",true);mob.add("requiredDefeats",new JsonArray());mob.add("requiredSeries",new JsonArray());
            mob.add("winCommands",new JsonObject());
            mob.addProperty("relativeLevelCap",-d.team().stream().mapToInt(CustomTrainer.Member::level).max().orElse(1));
            mob.addProperty("maxTrainerWins",-1);mob.addProperty("maxTrainerDefeats",-1);mob.addProperty("battleCooldownTicks",240);mob.addProperty("spawnWeightFactor",0);
            put(out,"mobs/trainers/single/"+d.id()+".json",mob);
            var dialogs=new JsonObject();
            dialog(dialogs,"on_battle_start",d.start());dialog(dialogs,"battle_start",d.start());
            dialog(dialogs,"on_battle_lost",d.playerWins());dialog(dialogs,"battle_lost",d.playerWins());dialog(dialogs,"trainer_lost",d.playerWins());
            dialog(dialogs,"on_battle_won",d.playerLoses());dialog(dialogs,"battle_won",d.playerLoses());dialog(dialogs,"trainer_won",d.playerLoses());
            put(out,"dialogs/trainers/single/"+d.id()+".json",dialogs);
        }
        return out;
    }
    private static JsonObject literal(String s){var o=new JsonObject();o.addProperty("literal",s);return o;}
    private static void dialog(JsonObject o,String key,String text){var a=new JsonArray();if(!text.isBlank())a.add(literal(text));o.add(key,a);}
    private static void put(Map<String,byte[]> out,String path,JsonObject json){out.put(path,json.toString().getBytes(StandardCharsets.UTF_8));}
}
