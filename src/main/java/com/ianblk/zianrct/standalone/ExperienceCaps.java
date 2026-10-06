package com.ianblk.zianrct.standalone;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.Priority;

/** Clamp incoming XP through Cobblemon's public event; never lowers existing levels. */
public final class ExperienceCaps {
    private ExperienceCaps(){}
    public static void register(){
        CobblemonEvents.EXPERIENCE_CANDY_USE_PRE.subscribe(Priority.LOWEST,
            (java.util.function.Consumer<com.cobblemon.mod.common.api.events.pokemon.interaction.ExperienceCandyUseEvent.Pre>)event->{
                var player=event.getPokemon().getOwnerPlayer();if(player==null)return;
                try{var progress=StandaloneRuntime.getInstance().getData(player);
                    if(progress.ledger().leagueActive() && event.getPokemon().getLevel()>=progress.getLevelCap())event.cancel();
                }catch(RuntimeException error){event.cancel();}
            });
        CobblemonEvents.EXPERIENCE_GAINED_EVENT_PRE.subscribe(Priority.LOWEST,
            (java.util.function.Consumer<com.cobblemon.mod.common.api.events.pokemon.ExperienceGainedEvent.Pre>)event->{
                var player=event.getPokemon().getOwnerPlayer();if(player==null || player.serverLevel().getServer()==null)return;
                try{
                    var progress=StandaloneRuntime.getInstance().getData(player);
                    if(!progress.ledger().leagueActive())return;
                    int cap=progress.getLevelCap();
                    if(cap>=100 || event.getExperience()<=0)return;
                    int ceiling=event.getPokemon().getExperienceGroup().getExperience(cap+1)-1;
                    int allowed=Math.max(0,ceiling-event.getPokemon().getExperience());
                    event.setExperience(Math.min(event.getExperience(),allowed));
                }catch(RuntimeException error){event.setExperience(0);com.ianblk.zianrct.ZianRCT.LOGGER.error("XP stopped because player progress could not be verified",error);}
            });
    }
}
