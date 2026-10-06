package com.ianblk.zianrct.creator;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** An independent challenge entity. Uses native battle creation, but not a series-membership prerequisite. */
public final class IndependentTrainerMob extends TrainerMob {
    public IndependentTrainerMob(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    @Override public boolean canBattleAgainst(Entity entity){
        if(!(entity instanceof Player player) || CustomTrainerStore.get(getTrainerId())==null)return false;
        var api=RCTMod.getInstance();var manager=api.getTrainerManager();
        return getCooldown()==0 && !isInBattle() && !api.isInBattle(player) && manager.getActivePokemon(player)>0
                && manager.getPlayerLevel(player)<=manager.getData(player).getLevelCap()
                && couldBattleAgainst(player);
    }
}
