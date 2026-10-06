package com.ianblk.zianrct.creator;
import java.util.*;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import net.minecraft.resources.ResourceLocation;

public final class AutomaticTrainerMoves {
    private AutomaticTrainerMoves() {}
    public static CustomTrainer prepare(CustomTrainer definition){
        if(!definition.autoMoves())return definition;
        List<CustomTrainer.Member> team=new ArrayList<>();
        for(var member:definition.team()){
            var id=ResourceLocation.tryParse(member.species().contains(":")?member.species():"cobblemon:"+member.species());
            var species=id==null?null:PokemonSpecies.getByIdentifier(id);
            if(species==null)throw new IllegalArgumentException("Especie desconocida: "+member.species());
            var learnset=species.getStandardForm().getMoves();
            var pool=new LinkedHashSet<MoveTemplate>(learnset.getLevelUpMovesUpTo(member.level()));
            if(definition.difficulty().equals("DIFICIL") || definition.difficulty().equals("JEFE")){
                pool.addAll(learnset.tmLearnableMoves());pool.addAll(learnset.getTutorMoves());
                pool.addAll(learnset.getEggMoves());pool.addAll(learnset.getEvolutionMoves());
            }
            var candidates=pool.stream().map(move->new AutomaticMovePolicy.Candidate(move.getName(),move.getElementalType().getName(),
                move.getPower(),move.getAccuracy(),move.getDamageCategory().getName().equalsIgnoreCase("status"),
                move.getElementalType().equals(species.getPrimaryType()) || move.getElementalType().equals(species.getSecondaryType()))).toList();
            var chosen=AutomaticMovePolicy.select(definition.difficulty(),candidates);
            if(chosen.isEmpty())throw new IllegalArgumentException("Sin movimientos disponibles a nivel "+member.level()+": "+member.species()+". Usa el modo manual.");
            team.add(new CustomTrainer.Member(member.species(),member.level(),chosen));
        }
        return new CustomTrainer(definition.id(),definition.name(),definition.difficulty(),definition.format(),definition.skin(),team,
                definition.start(),definition.playerWins(),definition.playerLoses(),true);
    }
}
