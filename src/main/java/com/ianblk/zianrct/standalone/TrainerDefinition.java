package com.ianblk.zianrct.standalone;

import com.gitlab.srcmc.rctapi.api.battle.*;
import com.gitlab.srcmc.rctapi.api.models.TrainerModel;
import com.google.gson.*;
import java.util.*;

/** Zian's data contract; only the team model is delegated to RCTAPI. */
public record TrainerDefinition(TrainerModel model,BattleFormat format,BattleRules rules,JsonObject mob,JsonObject dialogs) {
    public Team getTrainerTeam(){return new Team(model,format);}
    public int getMaxTrainerWins(){return integer(mob,"maxTrainerWins",-1);}
    public int getMaxTrainerDefeats(){return integer(mob,"maxTrainerDefeats",1);}
    public int cooldown(){return Math.max(0,integer(mob,"battleCooldownTicks",240));}
    public int level(){return model.getTeam().stream().mapToInt(p->p.getLevel()).max().orElse(1);}
    static int integer(JsonObject value,String key,int fallback){return value.has(key)?value.get(key).getAsInt():fallback;}
    public record Team(TrainerModel model,BattleFormat format){
        public com.gitlab.srcmc.rctapi.api.util.Text getName(){return model.getName();}
        public BattleFormat getBattleFormat(){return format;}
    }
}
