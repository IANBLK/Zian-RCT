package com.ianblk.zianrct.creator;
import java.util.*;

/** Deterministic initial balance, not a competitive team optimiser. */
public final class AutomaticMovePolicy {
    public record Candidate(String id,String type,double power,double accuracy,boolean status,boolean stab) {}
    private static final List<String> SUPPORT=List.of("recover","roost","slackoff","softboiled","synthesis","moonlight","morningsun","strengthsap","protect","detect","spore","willowisp","thunderwave","swordsdance","nastyplot","calmmind","dragondance","bulkup","quiverdance");
    private AutomaticMovePolicy() {}
    public static List<String> select(String difficulty,Collection<Candidate> pool){
        boolean easy=difficulty.equals("FACIL"),advanced=difficulty.equals("DIFICIL")||difficulty.equals("JEFE");
        var unique=new TreeMap<String,Candidate>();pool.forEach(c->unique.putIfAbsent(c.id(),c));
        Comparator<Candidate> attacks=Comparator.comparingDouble((Candidate c)->{
            double accuracy=c.accuracy()<=0?100:Math.min(100,c.accuracy());
            return (c.power()<=0?40:c.power())*accuracy/100*(c.stab()?1.5:1);
        });
        if(!easy)attacks=attacks.reversed();attacks=attacks.thenComparing(Candidate::id);
        var damaging=unique.values().stream().filter(c->!c.status()).sorted(attacks).toList();
        var support=unique.values().stream().filter(Candidate::status)
            .sorted(Comparator.comparingInt((Candidate c)->{int i=SUPPORT.indexOf(c.id());return i<0?999:i;}).thenComparing(Candidate::id)).toList();
        var selected=new LinkedHashSet<String>();var types=new HashSet<String>();
        int attackLimit=advanced && !support.isEmpty()?3:4;
        for(var c:damaging)if(selected.size()<attackLimit && types.add(c.type()))selected.add(c.id());
        for(var c:damaging)if(selected.size()<attackLimit)selected.add(c.id());
        for(var c:support)if(selected.size()<4)selected.add(c.id());
        return List.copyOf(selected);
    }
}
