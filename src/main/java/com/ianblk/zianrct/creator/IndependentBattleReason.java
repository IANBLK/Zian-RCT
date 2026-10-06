package com.ianblk.zianrct.creator;

/** Mirrors the independent entity's own battle gates, without league-series prerequisites. */
public final class IndependentBattleReason {
    private IndependentBattleReason() {}
    public static String context(boolean npcBusy,boolean playerBusy,int cooldown,int activePokemon,boolean aboveCap,boolean allowedRematch){
        if(npcBusy)return "trainer_busy";
        if(playerBusy)return "player_busy";
        if(cooldown>0)return "on_cooldown";
        if(activePokemon<=0)return "missing_pokemon";
        if(aboveCap)return "over_level_cap";
        if(!allowedRematch)return "done_generic";
        return "unknown_reason";
    }
}
