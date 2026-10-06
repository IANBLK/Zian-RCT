package com.ianblk.zianrct.creator;
import java.util.*;
public record CustomTrainer(String id,String name,String difficulty,String format,int skin,
                            List<Member> team,String start,String playerWins,String playerLoses) {
    public static final List<String> SKINS=List.of("rctmod:textures/trainers/default.png",
            "rctmod:textures/trainers/single/youngster_austin_0303.png",
            "rctmod:textures/trainers/single/youngster_ben_0059.png",
            "rctmod:textures/trainers/single/youngster_ben_0065.png");
    public record Member(String species,int level,List<String> moves) {
        public Member {
            if(species==null || !species.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_]+") || species.length()>96 || level<1 || level>100)
                throw new IllegalArgumentException("Pokémon o nivel inválido (1–100).");
            moves=List.copyOf(moves);
            if(moves.isEmpty() || moves.size()>4 || moves.stream().anyMatch(m->!m.matches("[a-z0-9_]{1,48}")))
                throw new IllegalArgumentException("Cada Pokémon necesita entre 1 y 4 movimientos válidos.");
        }
    }
    public CustomTrainer {
        if(id==null || !id.matches("zian_custom_[a-z0-9_]{1,48}") || name==null || name.isBlank() || name.length()>64
                || !Set.of("FACIL","NORMAL","DIFICIL","JEFE").contains(difficulty)
                || !Set.of("GEN_9_SINGLES","GEN_9_DOUBLES").contains(format) || skin<0 || skin>=SKINS.size())
            throw new IllegalArgumentException("Identidad, dificultad, formato o skin inválidos.");
        team=List.copyOf(team);
        if(team.isEmpty() || team.size()>6 || (format.equals("GEN_9_DOUBLES") && team.size()<2))
            throw new IllegalArgumentException("Equipo: 1–6 Pokémon; dobles requiere al menos 2.");
        for(String text:List.of(start,playerWins,playerLoses)) if(text.length()>256 || text.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Diálogo inválido (máximo 256 caracteres por frase).");
    }
    public CustomTrainer format(String format){return new CustomTrainer(id,name,difficulty,format,skin,team,start,playerWins,playerLoses);}
}
