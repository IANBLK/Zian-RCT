package com.ianblk.zianrct.client;
import com.google.gson.JsonParser;
import com.ianblk.zianrct.creator.CustomTrainer;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public final class CustomTrainerSkinState {
    private static Map<String,String> skins=Map.of();
    private CustomTrainerSkinState() {}
    public static void accept(String json){
        var obj=JsonParser.parseString(json).getAsJsonObject();
        if(obj.size()>64)throw new IllegalArgumentException("Too many skins");
        Map<String,String> next=new HashMap<>();
        for(var e:obj.entrySet()){
            String value=e.getValue().getAsString();
            if(!e.getKey().matches("zian_custom_[a-z0-9_]{1,48}") || !CustomTrainer.SKINS.contains(value))
                throw new IllegalArgumentException("Unsupported skin");
            next.put(e.getKey(),value);
        }
        skins=Map.copyOf(next);
    }
    public static ResourceLocation get(String id){String value=skins.get(id);return value==null?null:ResourceLocation.parse(value);}
    public static void clear(){skins=Map.of();}
}
