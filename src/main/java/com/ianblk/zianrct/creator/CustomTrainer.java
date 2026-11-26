package com.ianblk.zianrct.creator;
import com.ianblk.zianrct.LocalizedException;
import java.util.*;
public record CustomTrainer(String id,String name,String difficulty,String format,int skin,
                            List<Member> team,String start,String playerWins,String playerLoses,boolean autoMoves,Trial trial) {
    public record Trial(String boss,int shinyDenominator){
        public Trial {if(boss==null || !boss.matches("zian_custom_[a-z0-9_]{1,48}") || shinyDenominator<1 || shinyDenominator>1000000)throw new LocalizedException.Argument("zianrct.err.bad_boss_shiny","Jefe o probabilidad shiny inválidos.");}
    }
    public CustomTrainer(String id,String name,String difficulty,String format,int skin,List<Member> team,String start,String playerWins,String playerLoses,boolean autoMoves){
        this(id,name,difficulty,format,skin,team,start,playerWins,playerLoses,autoMoves,null);
    }
    public CustomTrainer(String id,String name,String difficulty,String format,int skin,List<Member> team,String start,String playerWins,String playerLoses){
        this(id,name,difficulty,format,skin,team,start,playerWins,playerLoses,false);
    }
    public static final List<String> SKIN_NAMES=List.of("Explorador Ártico", "Centinela Nocturno",
            "Guardián Cian", "Aventurero del Desierto", "Guardabosques", "Capitán Ámbar","Alaric del Zafiro","Soren del Hielo","Nyx del Umbral","Iris Prismática","Kael del Invierno","Elian del Alba","Darian del Roble","Varek Carmesí","Liora de Arena","Vigía del Sculk","Morgana de la Cosecha","Heraldo del Vacío","Arlequín Estelar","Sombra Violeta","Centinela Carmesí","Nadir de Medianoche","Guardia de Cobalto","Serafina de las Nieves","Lucien del Ocaso","Mika Rosada","Espectro del Sculk","Ignis del Brasero","Noir del Eclipse","Silas de Obsidiana","Amelia de las Flores","Sol del Ámbar","Magnus de Cobre");
    public static final List<String> SKINS=List.of("arctic_explorer", "night_sentinel", "cyan_guardian",
            "desert_adventurer", "forest_ranger", "amber_captain","alaric_zafiro","soren_hielo","nyx_umbral","iris_prismatica","kael_invierno","elian_alba","darian_roble","varek_carmesi","liora_arena","vigia_sculk","morgana_cosecha","heraldo_vacio","arlequin_estelar","sombra_violeta","centinela_carmesi","nadir_medianoche","guardia_cobalto","serafina_nieves","lucien_ocaso","mika_rosada","espectro_sculk","ignis_brasero","noir_eclipse","silas_obsidiana","amelia_flores","sol_ambar","magnus_cobre").stream()
            .map(name -> "zianrct:textures/entity/trainers/"+name+".png").toList();
    public static boolean slim(String texture){return Set.of("zianrct:textures/entity/trainers/alaric_zafiro.png","zianrct:textures/entity/trainers/soren_hielo.png","zianrct:textures/entity/trainers/nyx_umbral.png","zianrct:textures/entity/trainers/kael_invierno.png","zianrct:textures/entity/trainers/elian_alba.png","zianrct:textures/entity/trainers/arlequin_estelar.png","zianrct:textures/entity/trainers/sombra_violeta.png","zianrct:textures/entity/trainers/centinela_carmesi.png","zianrct:textures/entity/trainers/nadir_medianoche.png","zianrct:textures/entity/trainers/guardia_cobalto.png","zianrct:textures/entity/trainers/serafina_nieves.png","zianrct:textures/entity/trainers/mika_rosada.png","zianrct:textures/entity/trainers/ignis_brasero.png","zianrct:textures/entity/trainers/noir_eclipse.png","zianrct:textures/entity/trainers/silas_obsidiana.png","zianrct:textures/entity/trainers/amelia_flores.png","zianrct:textures/entity/trainers/sol_ambar.png","zianrct:textures/entity/trainers/magnus_cobre.png").contains(texture);}
    public record Member(String species,int level,List<String> moves) {
        public Member {
            if(species==null || !species.matches("(?:[a-z0-9_.-]+:)?[a-z0-9_]+") || species.length()>96 || level<1 || level>100)
                throw new LocalizedException.Argument("zianrct.err.bad_pokemon_level","Pokémon o nivel inválido (1–100).");
            moves=List.copyOf(moves);
            if(moves.isEmpty() || moves.size()>4 || moves.stream().anyMatch(m->!m.matches("[a-z0-9_]{1,48}")))
                throw new LocalizedException.Argument("zianrct.err.bad_moves","Cada Pokémon necesita entre 1 y 4 movimientos válidos.");
        }
    }
    public CustomTrainer {
        if(id==null || !id.matches("zian_custom_[a-z0-9_]{1,48}") || name==null || name.isBlank() || name.length()>64
                || !Set.of("FACIL","NORMAL","DIFICIL","JEFE").contains(difficulty)
                || !Set.of("GEN_9_SINGLES","GEN_9_DOUBLES").contains(format) || skin<0 || skin>=SKINS.size())
            throw new LocalizedException.Argument("zianrct.err.bad_identity","Identidad, dificultad, formato o skin inválidos.");
        team=List.copyOf(team);
        if(team.isEmpty() || team.size()>6 || (format.equals("GEN_9_DOUBLES") && team.size()<2))
            throw new LocalizedException.Argument("zianrct.err.bad_team","Equipo: 1–6 Pokémon; dobles requiere al menos 2.");
        for(String text:List.of(start,playerWins,playerLoses)) if(text.length()>256 || text.chars().anyMatch(Character::isISOControl))
            throw new LocalizedException.Argument("zianrct.err.bad_dialogue","Diálogo inválido (máximo 256 caracteres por frase).");
    }
    public CustomTrainer format(String format){return new CustomTrainer(id,name,difficulty,format,skin,team,start,playerWins,playerLoses,autoMoves,trial);}
}
