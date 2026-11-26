package com.ianblk.zianrct.creator;
import com.ianblk.zianrct.LocalizedException;
public final class CustomTrainerValidation {
    private CustomTrainerValidation() {}
    public static void validate(CustomTrainer trainer){
        for(var member:trainer.team()){
            var id=net.minecraft.resources.ResourceLocation.tryParse(member.species().contains(":")?member.species():"cobblemon:"+member.species());
            if(id==null || com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByIdentifier(id)==null)
                throw new LocalizedException.Argument("zianrct.err.unknown_species","Especie desconocida: %s",member.species());
            for(String move:member.moves())if(com.cobblemon.mod.common.api.moves.Moves.getByName(move)==null)
                throw new LocalizedException.Argument("zianrct.err.unknown_move","Movimiento desconocido: %s",move);
        }
    }
}
