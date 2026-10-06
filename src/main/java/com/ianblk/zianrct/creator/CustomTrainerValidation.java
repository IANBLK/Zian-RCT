package com.ianblk.zianrct.creator;
public final class CustomTrainerValidation {
    private CustomTrainerValidation() {}
    public static void validate(CustomTrainer trainer){
        for(var member:trainer.team()){
            var id=net.minecraft.resources.ResourceLocation.tryParse(member.species().contains(":")?member.species():"cobblemon:"+member.species());
            if(id==null || com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByIdentifier(id)==null)
                throw new IllegalArgumentException("Especie desconocida: "+member.species());
            for(String move:member.moves())if(com.cobblemon.mod.common.api.moves.Moves.getByName(move)==null)
                throw new IllegalArgumentException("Movimiento desconocido: "+move);
        }
    }
}
