package com.ianblk.zianrct;
import java.util.Locale;
/** Mensaje traducible sin dependencias de Minecraft: clave, texto de respaldo (español) y argumentos. */
public record Msg(String key,String fallback,Object... args){
    public String plain(){
        Object[] shown=args.clone();
        for(int i=0;i<shown.length;i++)if(shown[i] instanceof Msg nested)shown[i]=nested.plain();
        return String.format(Locale.ROOT,fallback,shown);
    }
}
