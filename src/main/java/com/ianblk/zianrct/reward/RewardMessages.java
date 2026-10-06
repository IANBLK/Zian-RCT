package com.ianblk.zianrct.reward;
public final class RewardMessages {
    private RewardMessages() {}
    private static String unit(long count,String one,String many){return count+" "+(count==1?one:many);}
    public static String waitFor(long millis) {
        long s=Math.max(1,(Math.max(0,millis)+999)/1000);
        if(s<60)return unit(s,"segundo","segundos");
        long m=(s+59)/60;
        if(m>=1440){long d=m/1440,h=m%1440/60;return unit(d,"día","días")+(h>0?" y "+unit(h,"hora","horas"):"");}
        if(m>=60){long h=m/60,part=m%60;return unit(h,"hora","horas")+(part>0?" y "+unit(part,"minuto","minutos"):"");}
        return unit(m,"minuto","minutos");
    }
    public static String delivered(String name,long waitMillis,boolean repeated) {
        String base=(repeated?"Recompensa repetible":"Recompensa")+" del entrenador "+name+" entregada.";
        if(!repeated)return base;
        return base+(waitMillis>0?" Vuelve dentro de "+waitFor(waitMillis)+" para recibir otra recompensa.":" Ya puedes volver a desafiarlo para obtener otra recompensa.");
    }
}
