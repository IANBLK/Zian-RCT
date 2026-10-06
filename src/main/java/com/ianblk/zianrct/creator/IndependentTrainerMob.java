package com.ianblk.zianrct.creator;
import com.gitlab.srcmc.rctmod.world.entities.TrainerMob;
import com.gitlab.srcmc.rctmod.api.RCTMod;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** An independent challenge entity. Uses native battle creation, but not a series-membership prerequisite. */
public final class IndependentTrainerMob extends TrainerMob {
    public IndependentTrainerMob(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    @Override protected void replyTo(Player player){
        var definition=CustomTrainerStore.get(getTrainerId());
        if(definition==null){super.replyTo(player);return;}
        if(player instanceof net.minecraft.server.level.ServerPlayer target){
            String blocked=com.ianblk.zianrct.legendary.LegendaryTrials.blocked(target,getTrainerId());
            if(blocked!=null){target.sendSystemMessage(net.minecraft.network.chat.Component.literal("<"+definition.name()+"> "+blocked));return;}
        }
        var api=RCTMod.getInstance();var manager=api.getTrainerManager();
        int cap=manager.getData(player).getLevelCap();
        String context=IndependentBattleReason.context(isInBattle(),api.isInBattle(player),getCooldown(),manager.getActivePokemon(player),
            manager.getPlayerLevel(player)>cap,definition.trial()!=null || couldBattleAgainst(player));
        com.gitlab.srcmc.rctmod.api.utils.ChatUtils.reply(this,player,context);
        if(context.equals("over_level_cap") && player instanceof net.minecraft.server.level.ServerPlayer target)
            target.sendSystemMessage(net.minecraft.network.chat.Component.literal("Tope actual de tu equipo: nivel "+cap+". Ser OP no elimina este límite de RCT."));
    }
    @Override public boolean canBattleAgainst(Entity entity){
        if(!(entity instanceof Player player) || CustomTrainerStore.get(getTrainerId())==null)return false;
        if(!com.ianblk.zianrct.legendary.LegendaryTrials.canStart(player.getUUID(),getTrainerId()))return false;
        if(CustomTrainerStore.get(getTrainerId()).trial()!=null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
            && !com.ianblk.zianrct.permission.RctPermissions.allows(serverPlayer.createCommandSourceStack(),"legendary.claim",false))return false;
        var api=RCTMod.getInstance();var manager=api.getTrainerManager();
        return getCooldown()==0 && !isInBattle() && !api.isInBattle(player) && manager.getActivePokemon(player)>0
                && manager.getPlayerLevel(player)<=manager.getData(player).getLevelCap()
                && (CustomTrainerStore.get(getTrainerId()).trial()!=null || couldBattleAgainst(player));
    }
}
