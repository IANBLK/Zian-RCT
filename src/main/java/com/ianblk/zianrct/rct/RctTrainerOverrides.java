package com.ianblk.zianrct.rct;
import com.google.gson.JsonParser;
import java.util.Set;

/** Change only the requested rules; preserve teams, AI, prerequisites and cooldown rules. */
public final class RctTrainerOverrides {
    private RctTrainerOverrides() {}
    public static String repeatMob(String source) {
        var json=JsonParser.parseString(source).getAsJsonObject();
        json.addProperty("maxTrainerDefeats",-1);json.addProperty("maxTrainerWins",-1);
        return json.toString();
    }
    public static String format(String source,String format) {
        if(!Set.of("GEN_9_SINGLES","GEN_9_DOUBLES").contains(format))throw new IllegalArgumentException("Formato no soportado");
        var json=JsonParser.parseString(source).getAsJsonObject();json.addProperty("battleFormat",format);return json.toString();
    }
}
