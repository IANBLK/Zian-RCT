package com.ianblk.zianrct.standalone;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.experience.SidemodExperienceSource;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.*;

/** Actual Cobblemon item/XP calls in the opt-in loopback integration fixture. */
public final class ExperienceCapsSmoke {
    private ExperienceCapsSmoke(){}
    public static void verify(MinecraftServer server,com.cobblemon.mod.common.api.battles.model.PokemonBattle battle) throws Exception {
        if(!"127.0.0.1".equals(server.getLocalIp()))throw new IllegalStateException("XP test requires loopback");
        UUID uuid=UUID.randomUUID();var player=FakePlayerFactory.get(server.overworld(),new GameProfile(uuid,"ZianCapSmoke"));
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
        @SuppressWarnings("unchecked") var players=(Map<UUID,ServerPlayer>)field.get(server.getPlayerList());
        players.put(uuid,player);
        try{
            player.getAbilities().instabuild=false;
            var runtime=StandaloneRuntime.getInstance();var progress=runtime.getData(player);
            if(progress.getLevelCap()!=10 || !progress.getDefeatedTrainerIds().isEmpty() || progress.ledger().leagueActive())throw new IllegalStateException("Not a fresh initial-cap fixture");
            var party=Cobblemon.INSTANCE.getStorage().getParty(player);party.clearParty();
            var pokemon=PokemonProperties.Companion.parse("pikachu level=1").create(player);party.add(pokemon);
            var xl=new ItemStack(CobblemonItems.EXPERIENCE_CANDY_XL,2);
            CobblemonItems.EXPERIENCE_CANDY_XL.applyToPokemon(player,xl,pokemon);
            if(pokemon.getLevel()!=10 || xl.getCount()!=1)throw new IllegalStateException("XL candy escaped the initial cap");
            int experience=pokemon.getExperience();
            CobblemonItems.EXPERIENCE_CANDY_XL.applyToPokemon(player,xl,pokemon);
            if(pokemon.getLevel()!=10 || pokemon.getExperience()!=experience || xl.getCount()!=1)throw new IllegalStateException("Blocked candy was consumed or changed XP");
            pokemon.setExperienceAndUpdateLevel(pokemon.getExperienceGroup().getExperience(9));
            var rare=new ItemStack(CobblemonItems.RARE_CANDY,2);
            CobblemonItems.RARE_CANDY.applyToPokemon(player,rare,pokemon);
            CobblemonItems.RARE_CANDY.applyToPokemon(player,rare,pokemon);
            if(pokemon.getLevel()!=10 || rare.getCount()!=1)throw new IllegalStateException("Rare candy escaped the initial cap");
            for(var source:List.<com.cobblemon.mod.common.api.pokemon.experience.ExperienceSource>of(
                    new SidemodExperienceSource("zianrct-smoke"),
                    new com.cobblemon.mod.common.api.pokemon.experience.CommandExperienceSource(player.createCommandSourceStack().withPermission(4)),
                    new com.cobblemon.mod.common.api.pokemon.experience.BattleExperienceSource(battle,List.of()))){
                pokemon.setExperienceAndUpdateLevel(pokemon.getExperienceGroup().getExperience(9));
                pokemon.addExperience(source,30000);
                if(pokemon.getLevel()!=10)throw new IllegalStateException("Experience source escaped the initial cap: "+source.getClass().getSimpleName());
            }
            progress.replaceChain(Set.of("rassvet_leader_novato"),Set.of("rassvet_leader_novato"));
            if(progress.getLevelCap()!=20)throw new IllegalStateException("Next cap not unlocked");
            var next=new ItemStack(CobblemonItems.EXPERIENCE_CANDY_XL,1);
            CobblemonItems.EXPERIENCE_CANDY_XL.applyToPokemon(player,next,pokemon);
            if(pokemon.getLevel()!=20 || next.getCount()!=0)throw new IllegalStateException("Training did not resume at the next cap");
            com.ianblk.zianrct.ZianRCT.LOGGER.info("Zian RCT initial cap smoke passed: XL candy, rare candy, battle/command/sidemod XP, unconsumed blocked items, unlocked cap");
        }finally{players.remove(uuid);}
    }
}
