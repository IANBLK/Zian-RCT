package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.Msg;
public final class RewardMessages {
    private RewardMessages() {}
    /** Nombre de la especie entregada; el adjetivo "shiny" va según el idioma (es: "%s shiny", en: "Shiny %s"). */
    public static Msg species(String species,boolean shiny){return shiny?new Msg("zianrct.legendary.shiny_species","%s shiny",species):new Msg("zianrct.legendary.plain_species","%s",species);}
    private static Msg unit(long count,String name,String one,String many){return new Msg("zianrct.time."+name+(count==1?"":"s"),"%s "+(count==1?one:many),count);}
    private static Msg and(Msg first,Msg second){return new Msg("zianrct.time.and","%s y %s",first,second);}
    public static Msg waitFor(long millis) {
        long s=Math.max(1,(Math.max(0,millis)+999)/1000);
        if(s<60)return unit(s,"second","segundo","segundos");
        long m=(s+59)/60;
        if(m>=1440){long d=m/1440,h=m%1440/60;return h>0?and(unit(d,"day","día","días"),unit(h,"hour","hora","horas")):unit(d,"day","día","días");}
        if(m>=60){long h=m/60,part=m%60;return part>0?and(unit(h,"hour","hora","horas"),unit(part,"minute","minuto","minutos")):unit(h,"hour","hora","horas");}
        return unit(m,"minute","minuto","minutos");
    }
    public static Msg delivered(String name,long waitMillis,boolean repeated) {
        if(!repeated)return new Msg("zianrct.reward.delivered","Recompensa del entrenador %s entregada.",name);
        return waitMillis>0?new Msg("zianrct.reward.delivered_wait","Recompensa repetible del entrenador %s entregada. Vuelve dentro de %s para recibir otra recompensa.",name,waitFor(waitMillis))
                :new Msg("zianrct.reward.delivered_ready","Recompensa repetible del entrenador %s entregada. Ya puedes volver a desafiarlo para obtener otra recompensa.",name);
    }
}
