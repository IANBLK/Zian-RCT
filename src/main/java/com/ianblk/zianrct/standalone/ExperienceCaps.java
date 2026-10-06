package com.ianblk.zianrct.standalone;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.Priority;

/** Clamp incoming XP through Cobblemon's public event; never lowers existing levels. */
public final class ExperienceCaps {
    private ExperienceCaps(){}
    public static void register(){
        CobblemonEvents.EXPERIENCE_CANDY_USE_PRE.subscribe(Priority.LOWEST,
            (java.util.function.Consumer<com.cobblemon.mod.common.api.events.pokemon.interaction.ExperienceCandyUseEvent.Pre>)event->{
                var player=event.getPlayer();if(player==null)return;
                try{var progress=StandaloneRuntime.getInstance().getData(player);
                    int cap=progress.getLevelCap();
                    var pokemon=event.getPokemon();
                    int allowed=ExperienceCapPolicy.allowed(pokemon.getLevel(),cap,pokemon.getExperience(),pokemon.getExperienceGroup().getExperience(cap),event.getExperienceYield());
                    if(pokemon.getLevel()>=cap){
                        event.cancel();
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(cap>=100?"Has alcanzado el nivel máximo de 100.":"Tu tope actual es nivel "+cap+". Supera el siguiente entrenador de la Liga para aumentarlo."));
                    }else event.setExperienceYield(allowed);
                }catch(RuntimeException error){event.cancel();}
            });
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.LOWEST,
            (java.util.function.Consumer<com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent.Pre>)event->{
                var player=event.getPokemon().getOwnerPlayer();if(player==null || player.serverLevel().getServer()==null)return;
                try{
                    var progress=StandaloneRuntime.getInstance().getData(player);
                    int cap=progress.getLevelCap();
                    if(event.getExperience()<=0)return;
                    var pokemon=event.getPokemon();
                    event.setExperience(ExperienceCapPolicy.allowed(pokemon.getLevel(),cap,pokemon.getExperience(),pokemon.getExperienceGroup().getExperience(cap),event.getExperience()));
                }catch(RuntimeException error){event.setExperience(0);com.ianblk.zianrct.ZianRCT.LOGGER.error("XP stopped because player progress could not be verified",error);}
            });
    }
}
