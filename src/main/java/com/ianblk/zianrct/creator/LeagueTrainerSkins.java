package com.ianblk.zianrct.creator;
import java.util.Map;

/** Original user-supplied league skins, keyed by stable trainer IDs. */
public final class LeagueTrainerSkins {
    private LeagueTrainerSkins(){}
    public static final Map<String,String> TEXTURES=Map.ofEntries(
        skin("rassvet_leader_novato","novato"),skin("rassvet_leader_ferrum","ferrum"),
        skin("rassvet_leader_aquila","aquila"),skin("rassvet_leader_voltar","voltar"),
        skin("rassvet_leader_engranaje","engranaje"),skin("rassvet_leader_bruma","bruma"),
        skin("rassvet_leader_cognitus","cognitus"),skin("rassvet_leader_forjax","forjax"),
        skin("rassvet_leader_glacius","glacius"),skin("rassvet_master_aurelia","aurelia")
    );
    private static Map.Entry<String,String> skin(String trainer,String name){return Map.entry(trainer,"zianrct:textures/entity/league/"+name+".png");}
    public static String texture(String trainer){return TEXTURES.get(trainer);}
}
