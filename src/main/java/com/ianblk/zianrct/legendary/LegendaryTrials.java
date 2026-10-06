package com.ianblk.zianrct.legendary;
import com.ianblk.zianrct.creator.*;
import com.ianblk.zianrct.ZianRCT;
import com.ianblk.zianrct.permission.RctPermissions;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory;
import com.cobblemon.mod.common.api.storage.adapter.flatfile.FileStoreAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.commands.Commands;
import java.util.*;

public final class LegendaryTrials {
    private static TrialJournal journal;
    private static MinecraftServer server;
    private static final Random RANDOM=new java.security.SecureRandom();
    private static final Map<String,Long> verifying=new HashMap<>();
    public LegendaryTrials(){
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e)->tick());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e)->e.getDispatcher().register(
            Commands.literal("zianrct").then(Commands.literal("legendary")
                .requires(s->RctPermissions.allows(s,"legendary.claim",false))
                .then(Commands.literal("status").executes(c->{status(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("claim").executes(c->{claim(c.getSource().getPlayerOrException());return 1;}))
                .then(Commands.literal("confirm-delivered").requires(s->RctPermissions.allows(s,"admin.legendary.resolve",true))
                    .then(Commands.argument("player",net.minecraft.commands.arguments.EntityArgument.player())
                        .then(Commands.argument("trainer",com.mojang.brigadier.arguments.StringArgumentType.word())
                            .then(Commands.argument("evidence",com.mojang.brigadier.arguments.StringArgumentType.greedyString()).executes(c->{
                                try{
                                    var player=net.minecraft.commands.arguments.EntityArgument.getPlayer(c,"player");
                                    String trainer=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"trainer"),evidence=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"evidence");
                                    if(journal==null || evidence.length()<8 || evidence.length()>256)throw new IllegalArgumentException("Registro no disponible o evidencia inválida (8–256 caracteres).");
                                    var entry=journal.get(player.getUUID(),trainer);
                                    if(entry==null || entry.phase()!=TrialJournal.Phase.REVIEW)throw new IllegalArgumentException("Solo permite cerrar una entrega en revisión, tras verificarla o compensarla manualmente.");
                                    journal.phase(player.getUUID(),trainer,TrialJournal.Phase.DELIVERED);
                                    ZianRCT.LOGGER.warn("[ZIAN-AUDIT] action=legendary_operator_confirmed admin={} player={} trainer={} pokemon={} evidence={}",c.getSource().getTextName(),player.getUUID(),trainer,entry.pokemon(),evidence);
                                    c.getSource().sendSuccess(()->Component.literal("Entrega cerrada. No se generó otro Pokémon."),true);return 1;
                                }catch(Exception error){c.getSource().sendFailure(Component.literal(error.getMessage()==null?"No se pudo resolver":error.getMessage()));return 0;}
                            }))))))));
    }
    public static void start(MinecraftServer s){server=s;journal=null;verifying.clear();ticks=0;smokePlayer=null;smokeBegan=0;smokeDone=false;
        try{journal=TrialJournal.open(s.getWorldPath(LevelResource.ROOT).resolve("data/zianrct-legendary-trials.json"));}
        catch(Exception e){ZianRCT.LOGGER.error("Legendary trials unavailable; journal preserved",e);}
    }
    public static void stop(){server=null;journal=null;verifying.clear();smokePlayer=null;}
    public static boolean interested(String trainer){return CustomTrainerStore.all().values().stream().anyMatch(d->d.trial()!=null && (d.id().equals(trainer)||d.trial().boss().equals(trainer)));}
    public static boolean canStart(UUID player,String trainer){
        var d=CustomTrainerStore.get(trainer);if(d==null || d.trial()==null)return true;
        try{validate(d);var entry=journal==null?null:journal.get(player,trainer);return entry!=null && entry.phase()==TrialJournal.Phase.UNLOCKED;}catch(Exception e){return false;}
    }
    public static String blocked(ServerPlayer player,String trainer){
        var d=CustomTrainerStore.get(trainer);if(d==null || d.trial()==null)return null;
        if(!RctPermissions.allows(player.createCommandSourceStack(),"legendary.claim",false))return "No tienes permiso para este desafío.";
        if(journal==null)return "El registro de pruebas no está disponible. Consulta al administrador.";
        try{var entry=journal.get(player.getUUID(),trainer);
            if(entry==null)return "Primero debes derrotar al jefe "+com.ianblk.zianrct.reward.TrainerRewardService.name(d.trial().boss())+".";
            return switch(entry.phase()){
                case UNLOCKED->null;
                case READY->"Ya ganaste este desafío. Tu Pokémon está pendiente: /zianrct legendary claim";
                case APPLYING,REVIEW->"La entrega de tu Pokémon necesita confirmación; no se repetirá automáticamente.";
                case DELIVERED->"Ya superaste la prueba y recibiste tu Pokémon. Este desafío es único.";
            };
        }catch(Exception e){return "No se pudo verificar tu prueba. Consulta al administrador.";}
    }
    public static void validate(CustomTrainer d){
        if(CustomTrainerStore.all().values().stream().anyMatch(t->t.trial()!=null && t.trial().boss().equals(d.id())) && (!d.difficulty().equals("JEFE") || d.trial()!=null))
            throw new IllegalArgumentException("Este entrenador desbloquea una prueba: debe seguir siendo JEFE y no puede convertirse en otra prueba.");
        if(d.trial()==null){
            if(journal!=null && journal.all().stream().anyMatch(e->e.trainer().equals(d.id())))throw new IllegalArgumentException("No elimines una prueba con progreso registrado.");
            return;
        }
        if(!d.format().equals("GEN_9_SINGLES") || d.team().size()!=1 || d.trial().boss().equals(d.id()))throw new IllegalArgumentException("La prueba requiere un solo legendario, batalla individual y otro jefe.");
        var boss=CustomTrainerStore.get(d.trial().boss());
        if(boss==null || !boss.difficulty().equals("JEFE") || boss.trial()!=null)throw new IllegalArgumentException("Selecciona un jefe propio de dificultad JEFE, sin otra prueba legendaria.");
        var m=d.team().getFirst();var species=PokemonSpecies.getByIdentifier(net.minecraft.resources.ResourceLocation.parse(m.species().contains(":")?m.species():"cobblemon:"+m.species()));
        if(species==null || !species.getImplemented() || !(species.getLabels().contains("legendary") || species.getLabels().contains("mythical")))throw new IllegalArgumentException("El equipo de esta prueba debe ser un Pokémon legendario o singular implementado.");
        if(journal==null)throw new IllegalArgumentException("Registro de pruebas no disponible.");
        for(var e:journal.all())if(e.trainer().equals(d.id()) && (!e.boss().equals(d.trial().boss()) || !e.species().equals(m.species()) || e.level()!=m.level() || e.shinyDenominator()!=d.trial().shinyDenominator()))
            throw new IllegalArgumentException("No cambies jefe, especie, nivel o shiny de una prueba que ya tiene progreso. Usa un ID nuevo.");
    }
    public static void won(ServerPlayer player,String trainer,UUID battle){
        if(journal==null){if(interested(trainer))player.sendSystemMessage(Component.literal("No se pudo registrar la prueba legendaria. Consulta al administrador antes de continuar."));return;}
        if(battle==null || server==null || !server.isSameThread())return;
        try{
            for(var d:CustomTrainerStore.all().values())if(d.trial()!=null && d.trial().boss().equals(trainer)){
                validate(d);
                var m=d.team().getFirst();
                if(journal.unlock(player.getUUID(),d.id(),trainer,m.species(),m.level(),d.trial().shinyDenominator(),battle)){
                    player.sendSystemMessage(Component.literal("Has superado la prueba. Puedes hablar con "+d.name()+" para luchar con el legendario."));
                    ZianRCT.LOGGER.info("[ZIAN-AUDIT] action=legendary_unlock player={} trainer={} boss={} battle={}",player.getUUID(),d.id(),trainer,battle);
                }
            }
            var d=CustomTrainerStore.get(trainer);
            if(d!=null && d.trial()!=null){var entry=journal.victory(player.getUUID(),trainer,battle,RANDOM);if(entry!=null && entry.phase()==TrialJournal.Phase.READY)deliver(player,entry);}
        }catch(Exception e){ZianRCT.LOGGER.error("Legendary trial mutation failed for {} / {}",player.getUUID(),trainer,e);player.sendSystemMessage(Component.literal("La prueba legendaria necesita revisión. No entregues otro Pokémon sin revisar el registro."));}
    }
    private record Storage(PCStore pc,FileBackedPokemonStoreFactory<?> factory,FileStoreAdapter<?> adapter){}
    private static Storage storage(ServerPlayer player) throws ReflectiveOperationException {
        var manager=Cobblemon.INSTANCE.getStorage();var pc=manager.getPC(player);
        // Read-only provider discovery for pinned Cobblemon 1.8.1. Mutations use its native APIs.
        var field=com.cobblemon.mod.common.api.storage.PokemonStoreManager.class.getDeclaredField("factories");field.setAccessible(true);
        for(Object object:(Iterable<?>)field.get(manager))if(object instanceof FileBackedPokemonStoreFactory<?> factory && factory.isCached(pc)
            && factory.getPC(player.getUUID(),player.registryAccess())==pc){
            var method=FileBackedPokemonStoreFactory.class.getDeclaredMethod("getAdapter");method.setAccessible(true);
            return new Storage(pc,factory,(FileStoreAdapter<?>)method.invoke(factory));
        }
        throw new IllegalStateException("Proveedor de PC sin confirmación de guardado compatible. Premio conservado pendiente.");
    }
    private static void deliver(ServerPlayer player,TrialJournal.Entry entry) throws Exception {
        var store=storage(player);
        if(store.pc().getFirstAvailablePosition()==null){player.sendSystemMessage(Component.literal("Tu PC está lleno. Libera espacio y usa /zianrct legendary claim; el premio está guardado."));return;}
        if(store.pc().get(entry.pokemon())!=null || Cobblemon.INSTANCE.getStorage().getParty(player).get(entry.pokemon())!=null){
            journal.phase(entry.player(),entry.trainer(),TrialJournal.Phase.APPLYING);journal.phase(entry.player(),entry.trainer(),TrialJournal.Phase.REVIEW);
            throw new IllegalStateException("El UUID reservado ya está presente; no se creará otro Pokémon");
        }
        var species=PokemonSpecies.getByIdentifier(net.minecraft.resources.ResourceLocation.parse(entry.species().contains(":")?entry.species():"cobblemon:"+entry.species()));
        if(species==null)throw new IllegalStateException("Especie pendiente no disponible");
        var pokemon=species.create(entry.level());pokemon.setUuid(entry.pokemon());pokemon.setShiny(entry.shiny());pokemon.setOriginalTrainer(player.getUUID());
        var stats=List.of(Stats.HP,Stats.ATTACK,Stats.DEFENCE,Stats.SPECIAL_ATTACK,Stats.SPECIAL_DEFENCE,Stats.SPEED);
        for(int i=0;i<6;i++)pokemon.setIV(stats.get(i),entry.ivs().get(i));
        journal.phase(entry.player(),entry.trainer(),TrialJournal.Phase.APPLYING);
        if(!store.pc().add(pokemon)){journal.phase(entry.player(),entry.trainer(),TrialJournal.Phase.REVIEW);return;}
        store.factory().save(store.pc(),player.registryAccess());
        verifying.put(entry.player()+":"+entry.trainer(),System.currentTimeMillis());
        player.sendSystemMessage(Component.literal("Has vencido al legendario. Tu Pokémon se añadió al PC; confirmando el guardado."));
    }
    private static boolean verify(ServerPlayer player,TrialJournal.Entry entry) throws Exception {
        var store=storage(player);var persisted=store.adapter().load(PCStore.class,store.pc().getUuid(),player.registryAccess());
        var savedPokemon=findPersisted(persisted,entry.pokemon());
        boolean present=savedPokemon!=null;
        if(!present){
            var party=Cobblemon.INSTANCE.getStorage().getParty(player);
            var savedParty=store.adapter().load(com.cobblemon.mod.common.api.storage.party.PlayerPartyStore.class,party.getUuid(),player.registryAccess());
            savedPokemon=findPersisted(savedParty,entry.pokemon());present=savedPokemon!=null;
        }
        if(!present)return false;
        String expected=entry.species().contains(":")?entry.species():"cobblemon:"+entry.species();
        if(!savedPokemon.getSpecies().getResourceIdentifier().toString().equals(expected) || savedPokemon.getShiny()!=entry.shiny())throw new IllegalStateException("El Pokémon guardado no coincide con el premio reservado");
        var stats=List.of(Stats.HP,Stats.ATTACK,Stats.DEFENCE,Stats.SPECIAL_ATTACK,Stats.SPECIAL_DEFENCE,Stats.SPEED);
        for(int i=0;i<6;i++)if(savedPokemon.getIvs().getOrDefault(stats.get(i))!=entry.ivs().get(i))throw new IllegalStateException("Los IV guardados no coinciden con el premio reservado");
        journal.phase(entry.player(),entry.trainer(),TrialJournal.Phase.DELIVERED);
        player.sendSystemMessage(Component.literal("Prueba completada: recibiste "+entry.species()+(entry.shiny()?" shiny":"")+" en tu PC. No podrás repetir esta recompensa."));
        ZianRCT.LOGGER.info("[ZIAN-AUDIT] action=legendary_delivered player={} trainer={} pokemon={} shiny={} ivs={} battle={}",entry.player(),entry.trainer(),entry.pokemon(),entry.shiny(),entry.ivs(),entry.victoryBattle());return true;
    }
    private static com.cobblemon.mod.common.pokemon.Pokemon findPersisted(com.cobblemon.mod.common.api.storage.PokemonStore<?> store,UUID uuid){
        // Adapter.load restores the boxes/slots, but does not initialise the live UUID cache.
        // Read the persisted members directly, without initialising or tracking a second live store.
        if(store!=null)for(var pokemon:store)if(pokemon.getUuid().equals(uuid))return pokemon;
        return null;
    }
    private static long ticks;
    private static ServerPlayer smokePlayer;
    private static long smokeBegan;
    private static boolean smokeDone;
    private static void smoke(){
        if(!"true".equals(System.getenv("ZIANRCT_LEGENDARY_SMOKE")) || smokeDone)return;
        String trainer="zian_custom_ci_legendary",boss="zian_custom_ci";
        if(!com.gitlab.srcmc.rctmod.api.RCTMod.getInstance().getTrainerManager().isValidId(trainer))return;
        try{
            if(smokePlayer==null){
                smokePlayer=new ServerPlayer(server,server.overworld(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ZianTrialSmoke"),net.minecraft.server.level.ClientInformation.createDefault()){
                    @Override public void sendSystemMessage(Component message){}
                };
                won(smokePlayer,boss,UUID.randomUUID());won(smokePlayer,trainer,UUID.randomUUID());smokeBegan=System.currentTimeMillis();
            }
            var entry=journal.get(smokePlayer.getUUID(),trainer);
            if(entry==null || entry.phase()==TrialJournal.Phase.UNLOCKED || entry.phase()==TrialJournal.Phase.READY || entry.phase()==TrialJournal.Phase.REVIEW)
                throw new IllegalStateException("Native legendary smoke did not start delivery");
            if(entry.phase()==TrialJournal.Phase.APPLYING && !verify(smokePlayer,entry)){
                if(System.currentTimeMillis()-smokeBegan>30000)throw new IllegalStateException("Native legendary smoke did not confirm persistence");return;
            }
            var uuid=entry.pokemon();won(smokePlayer,trainer,UUID.randomUUID());
            if(!journal.get(smokePlayer.getUUID(),trainer).pokemon().equals(uuid) || canStart(smokePlayer.getUUID(),trainer))throw new IllegalStateException("Completed trial reopened");
            smokeDone=true;ZianRCT.LOGGER.info("Zian RCT legendary storage smoke delivered and replay blocked: {}",uuid);
        }catch(Exception error){smokeDone=true;ZianRCT.LOGGER.error("Zian RCT legendary storage smoke failed",error);}
    }
    private static void tick(){
        if(server==null || journal==null || ++ticks%20!=0)return;
        smoke();
        try{for(var entry:journal.all())if(entry.phase()==TrialJournal.Phase.APPLYING){
            var player=server.getPlayerList().getPlayer(entry.player());if(player==null)continue;
            String key=entry.player()+":"+entry.trainer();
            try{if(verify(player,entry)){verifying.remove(key);continue;}}
            catch(Exception e){ZianRCT.LOGGER.warn("Legendary delivery readback failed: {}",key,e);}
            Long began=verifying.get(key);
            if(began==null || System.currentTimeMillis()-began>10000){journal.phase(entry.player(),entry.trainer(),TrialJournal.Phase.REVIEW);verifying.remove(key);}
        }}catch(Exception e){ZianRCT.LOGGER.error("Legendary trial verification unavailable",e);journal=null;}
    }
    public static void claim(ServerPlayer player){
        if(journal==null){player.sendSystemMessage(Component.literal("Registro de pruebas no disponible."));return;}
        if(!RctPermissions.allows(player.createCommandSourceStack(),"legendary.claim",false))return;
        try{for(var entry:journal.all())if(entry.player().equals(player.getUUID())){
            if(entry.phase()==TrialJournal.Phase.READY)deliver(player,entry);
            else if(entry.phase()==TrialJournal.Phase.APPLYING || entry.phase()==TrialJournal.Phase.REVIEW)verify(player,entry);
        }}catch(Exception e){player.sendSystemMessage(Component.literal("Premio conservado pendiente o en revisión: "+e.getMessage()));ZianRCT.LOGGER.warn("Legendary claim deferred for {}",player.getUUID(),e);}
    }
    public static void status(ServerPlayer player){
        if(journal==null){player.sendSystemMessage(Component.literal("Registro de pruebas no disponible."));return;}
        try{var own=journal.all().stream().filter(e->e.player().equals(player.getUUID())).toList();
            if(own.isEmpty())player.sendSystemMessage(Component.literal("Aún no has desbloqueado pruebas legendarias."));
            for(var e:own){String state=switch(e.phase()){
                case UNLOCKED->"Prueba superada; combate disponible";
                case READY->"Victoria obtenida; premio pendiente";
                case APPLYING->"Confirmando entrega";
                case DELIVERED->"Pokémon recibido; prueba cerrada";
                case REVIEW->"Entrega en revisión";
            };player.sendSystemMessage(Component.literal(com.ianblk.zianrct.reward.TrainerRewardService.name(e.trainer())+" | "+state+" | "+e.species()));}
        }catch(Exception e){player.sendSystemMessage(Component.literal("No se pudo leer el registro de pruebas."));}
    }
}
