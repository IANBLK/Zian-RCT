package com.ianblk.zianrct.creator;

import com.ianblk.zianrct.standalone.StandaloneRuntime;
import com.ianblk.zianrct.config.ConfigState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Zian-owned entity; preserves legacy NBT keys for custom NPCs. */
public final class IndependentTrainerMob extends PathfinderMob implements net.minecraft.world.entity.npc.Npc {
    private static final EntityDataAccessor<String> TRAINER=SynchedEntityData.defineId(IndependentTrainerMob.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Component> SPEECH=SynchedEntityData.defineId(IndependentTrainerMob.class,EntityDataSerializers.COMPONENT);
    private int speechTicks;
    private int cooldown;
    private BlockPos home;
    private boolean persistent=true;
    public IndependentTrainerMob(EntityType<? extends PathfinderMob> type,Level level){super(type,level);}
    public static EntityType<IndependentTrainerMob> getEntityType(){return CustomTrainerEntities.TYPE.get();}
    public static AttributeSupplier.Builder createAttributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,0.25).add(Attributes.FOLLOW_RANGE,16);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(TRAINER,"");builder.define(SPEECH,Component.empty());}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(5,new RandomStrollGoal(this,0.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
    public Component getSpeech(){return entityData.get(SPEECH);}
    @Override public boolean shouldBeSaved(){return persistent;}
    public String getTrainerId(){return entityData.get(TRAINER);}
    public void setTrainerId(String id){entityData.set(TRAINER,id);}
    public BlockPos getHomePos(){return home==null?blockPosition():home;}
    public void setHomePos(BlockPos pos){home=pos.immutable();restrictTo(home,8);}
    public int getCooldown(){return cooldown;}
    public boolean isInBattle(){return !level().isClientSide && StandaloneRuntime.getInstance().busy(getUUID());}
    public void setPersistent(boolean value){persistent=value;}
    @Override public boolean isPersistenceRequired(){return persistent;}
    @Override public boolean removeWhenFarAway(double distance){return !persistent && !isInBattle();}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public void tick(){super.tick();if(!level().isClientSide){
        if(cooldown>0)cooldown--;
        if(speechTicks>0 && --speechTicks==0)entityData.set(SPEECH,Component.empty());
        if(tickCount%20==0 && StandaloneRuntime.getInstance().isValidId(getTrainerId())){
            Component name=StandaloneRuntime.getInstance().getData(getTrainerId()).model().getName().getComponent();
            if(!name.equals(getCustomName()))setCustomName(name);setCustomNameVisible(true);
        }
    }}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putString("TrainerId",getTrainerId());tag.putInt("Cooldown",cooldown);tag.putBoolean("Persistent",persistent);tag.putLong("HomePos",getHomePos().asLong());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);setTrainerId(tag.getString("TrainerId"));cooldown=Math.max(0,tag.getInt("Cooldown"));
        persistent=!tag.contains("Persistent") || tag.getBoolean("Persistent");if(tag.contains("HomePos"))setHomePos(BlockPos.of(tag.getLong("HomePos")));
    }
    @Override public Component getDisplayName(){
        if(level().isClientSide)return super.getDisplayName();
        try{return StandaloneRuntime.getInstance().getData(getTrainerId()).model().getName().getComponent();}catch(RuntimeException error){return Component.translatableWithFallback("zianrct.trainer.default_name","Entrenador Zian");}
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        if(hand==InteractionHand.MAIN_HAND && player instanceof ServerPlayer target)startBattleWith(target);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    public boolean couldBattleAgainst(Entity entity){return blocked(entity)==null;}
    public boolean canBattleAgainst(Entity entity){return blocked(entity)==null;}
    private String blocked(Entity entity){
        if(!(entity instanceof ServerPlayer player))return "unknown_reason";
        var runtime=StandaloneRuntime.getInstance();
        if(!runtime.isValidId(getTrainerId()))return "unknown_reason";
        if(isInBattle())return "trainer_busy";if(runtime.isInBattle(player))return "player_busy";if(cooldown>0)return "on_cooldown";
        if(runtime.getActivePokemon(player)<(runtime.getData(getTrainerId()).format().name().contains("DOUBLES")?2:1))return "missing_pokemon";
        var progress=runtime.getData(player);
        if(runtime.isLeague(getTrainerId()) && runtime.getPlayerLevel(player)>progress.getLevelCap())return "over_level_cap";
        var profile=ConfigState.current().activeProfileConfig();
        for(int i=0;i<profile.chain().size();i++)if(profile.chain().get(i).trainer().equals(getTrainerId())){
            if(i>0 && !progress.getDefeatedTrainerIds().contains(profile.chain().get(i-1).trainer()))return "missing_required_trainer";
            int required=i==0?profile.initialCap():profile.unlockCap(i-1);
            if(progress.getLevelCap()<required)return "low_level_cap";
        }
        var data=runtime.getData(getTrainerId());
        if(data.getMaxTrainerDefeats()!=-1 && progress.getDefeatedTrainerIds().contains(getTrainerId()))return "done_generic";
        if(data.getMaxTrainerWins()!=-1 && progress.ledger().losses(getTrainerId())>=data.getMaxTrainerWins())return "done_generic";
        return null;
    }
    public void startBattleWith(Player player){
        if(!(player instanceof ServerPlayer target))return;
        try{
            Component trial=com.ianblk.zianrct.reward.TrainerRewardService.battleBlock(target,getTrainerId());
            if(trial!=null){say(target,trial);return;}
            if(!com.ianblk.zianrct.permission.RctPermissions.allows(target.createCommandSourceStack(),"battle",false)){say(target,Component.translatableWithFallback("zianrct.trainer.no_permission","No tienes permiso para iniciar este desafío."));return;}
            var own=CustomTrainerStore.get(getTrainerId());
            if(own!=null && own.trial()!=null && !com.ianblk.zianrct.permission.RctPermissions.allows(target.createCommandSourceStack(),"legendary.claim",false)){
                say(target,Component.translatableWithFallback("zianrct.trainer.no_trial_permission","No tienes permiso para participar en esta prueba legendaria."));return;
            }
            String reason=blocked(target);if(reason!=null){reply(target,reason);return;}
            if(StandaloneRuntime.getInstance().start(this,target))reply(target,"on_battle_start");else reply(target,"unknown_reason");
        }catch(RuntimeException error){com.ianblk.zianrct.ZianRCT.LOGGER.error("Could not start trainer battle {}",getTrainerId(),error);say(target,Component.translatableWithFallback("zianrct.trainer.start_failed","No se pudo iniciar el combate. Consulta al administrador."));}
    }
    public void finishBattle(){cooldown=StandaloneRuntime.getInstance().getData(getTrainerId()).cooldown();}
    public void reply(ServerPlayer player,String context){
        var messages=StandaloneRuntime.getInstance().getData(getTrainerId()).dialogs().getAsJsonArray(context);
        if(messages!=null && !messages.isEmpty()){
            var message=messages.get(random.nextInt(messages.size()));
            if(message.isJsonPrimitive())say(player,message.getAsString());
            else{var text=message.getAsJsonObject();if(text.has("literal")&&text.has("translatable")&&text.get("translatable").getAsString().startsWith("zianrct.trainer.reply.")&&!text.get("translatable").getAsString().equals(text.get("literal").getAsString()))say(player,Component.translatableWithFallback(text.get("translatable").getAsString(),text.get("literal").getAsString()));else if(text.has("literal"))say(player,text.get("literal").getAsString());else if(text.has("translatable"))player.sendSystemMessage(com.ianblk.zianrct.Messages.format(com.ianblk.zianrct.npc.NpcDialogueFormat.line(getDisplayName(),Component.translatable(text.get("translatable").getAsString()))));}
        }else say(player,switch(context){
            case "trainer_busy"->Component.translatableWithFallback("zianrct.trainer.busy","Estoy en otro combate. Espera a que termine.");
            case "player_busy"->Component.translatableWithFallback("zianrct.trainer.player_busy","Termina tu combate actual antes de retarme.");
            case "on_cooldown"->Component.translatableWithFallback("zianrct.trainer.cooldown","Nuestros equipos necesitan descansar un momento.");
            case "missing_pokemon"->Component.translatableWithFallback("zianrct.trainer.missing_pokemon","Necesitas suficientes Pokémon capaces de combatir para este formato.");
            case "over_level_cap"->Component.translatableWithFallback("zianrct.trainer.over_level_cap","Tu equipo supera tu tope actual: nivel %s.",StandaloneRuntime.getInstance().getData(player).getLevelCap());
            case "done_generic"->Component.translatableWithFallback("zianrct.trainer.done","Ya has completado este encuentro.");
            default->Component.translatableWithFallback("zianrct.trainer.cannot_start","No puedes iniciar este desafío en este momento.");
        });
    }
    private void say(ServerPlayer player,Component text){String plain=com.ianblk.zianrct.Messages.plain(text);if(!plain.isBlank()){entityData.set(SPEECH,text);speechTicks=120;player.sendSystemMessage(com.ianblk.zianrct.Messages.format(com.ianblk.zianrct.npc.NpcDialogueFormat.line(getDisplayName(),text)));}}
    private void say(ServerPlayer player,String text){if(!text.isBlank()){entityData.set(SPEECH,Component.literal(text.substring(0,Math.min(512,text.length()))));speechTicks=120;player.sendSystemMessage(com.ianblk.zianrct.Messages.format(com.ianblk.zianrct.npc.NpcDialogueFormat.line(getDisplayName(),Component.literal(text))));}}
}
