package com.ianblk.zianrct.client;
import static com.ianblk.zianrct.Messages.t;
import com.ianblk.zianrct.npc.*;
import com.ianblk.zianrct.creator.*;
import com.google.gson.Gson;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class CustomTrainerScreen extends ResponsiveScreen {
    private final NpcEditorState state;
    private String id="zian_custom_reto",name="Guardia del reto",difficulty="NORMAL",format="GEN_9_SINGLES";
    private int skin,tab,scroll,x,y,w,h,contentHeight;
    private String start="¿Listo para este reto?",win="Buen combate. Me has derrotado.",loss="Practica y vuelve a intentarlo.";
    private final String[] species=new String[6],levels=new String[6],moves=new String[6];
    private final Map<AbstractWidget,Integer> rows=new LinkedHashMap<>();
    private final List<Label> labels=new ArrayList<>();
    private final List<Runnable> reads=new ArrayList<>();
    private Button save,back;
    private String error="";
    private boolean autoMoves=true;
    private boolean trialEnabled;
    private String trialBoss="",shinyDenominator="512";
    private record Label(String text,int row){}
    public CustomTrainerScreen(NpcEditorState state){
        super(Component.translatableWithFallback("ui.zianrct.58de124576e5", "Zian RCT · Definir entrenador"));this.state=state;
        Arrays.fill(species,"");Arrays.fill(levels,"25");Arrays.fill(moves,"tackle");
        species[0]="pikachu";moves[0]="tackle,thunderbolt";
        if(!state.customDraft().isBlank())load(CustomTrainerStore.decode(state.customDraft()));
        var previous=net.minecraft.client.Minecraft.getInstance().screen;
        if(!state.notice().isBlank() && previous instanceof CustomTrainerScreen p){p.read();copy(p);error=state.notice();}
    }
    private void load(CustomTrainer d){
        id=d.id();name=d.name();difficulty=d.difficulty();format=d.format();skin=d.skin();start=d.start();win=d.playerWins();loss=d.playerLoses();
        autoMoves=d.autoMoves();
        trialEnabled=d.trial()!=null;if(trialEnabled){trialBoss=d.trial().boss();shinyDenominator=Integer.toString(d.trial().shinyDenominator());}
        Arrays.fill(species,"");
        for(int i=0;i<d.team().size();i++){var m=d.team().get(i);species[i]=m.species();levels[i]=Integer.toString(m.level());moves[i]=String.join(",",m.moves());}
    }
    private void copy(CustomTrainerScreen p){
        id=p.id;name=p.name;difficulty=p.difficulty;format=p.format;skin=p.skin;start=p.start;win=p.win;loss=p.loss;tab=p.tab;scroll=p.scroll;
        autoMoves=p.autoMoves;
        trialEnabled=p.trialEnabled;trialBoss=p.trialBoss;shinyDenominator=p.shinyDenominator;
        System.arraycopy(p.species,0,species,0,6);System.arraycopy(p.levels,0,levels,0,6);System.arraycopy(p.moves,0,moves,0,6);
    }
    private void read(){reads.forEach(Runnable::run);}
    @Override protected void init(){fitViewport(600,360);
        rows.clear();labels.clear();reads.clear();clearWidgets();
        w=Math.min(540,width-16);h=Math.min(320,height-16);x=(width-w)/2;y=(height-h)/2;
        int left=x+12,usable=w-24;
        for(int i=0;i<4;i++){int selected=i;button(left+i*(usable/4),0,usable/4-4,List.of(t("zianrct.designer.tab.identity","Identidad"),t("zianrct.designer.tab.team","Equipo"),t("zianrct.designer.tab.dialogues","Diálogos"),t("zianrct.designer.tab.test","Prueba")).get(i),()->{read();tab=selected;scroll=0;init();});}
        if(tab==0){
            label(t("zianrct.designer.own_id","ID propio (prefijo zian_custom_):"),26);
            EditBox idBox=field(left,40,usable,64,id);idBox.setEditable(state.trainer().isEmpty());reads.add(()->id=idBox.getValue());
            label(t("zianrct.designer.visible_name","Nombre visible:"),64);EditBox n=field(left,78,usable,64,name);reads.add(()->name=n.getValue());
            button(left,102,usable,t("zianrct.designer.difficulty","Dificultad: %s",difficulty),()->{read();var all=List.of("FACIL","NORMAL","DIFICIL","JEFE");difficulty=all.get((all.indexOf(difficulty)+1)%4);init();});
            button(left,126,usable,t("zianrct.designer.battle","Batalla: %s",format.equals("GEN_9_DOUBLES")?t("zianrct.designer.double","Doble"):t("zianrct.designer.single","Individual")),()->{read();format=format.equals("GEN_9_DOUBLES")?"GEN_9_SINGLES":"GEN_9_DOUBLES";init();});
            button(left,150,usable,t("zianrct.designer.skin","Skin: %s",CustomTrainer.SKIN_NAMES.get(skin)),()->{read();var options=new java.util.ArrayList<TrainerChoiceScreen.Choice>();for(int at=0;at<CustomTrainer.SKINS.size();at++){String texture=CustomTrainer.SKINS.get(at);options.add(new TrainerChoiceScreen.Choice(""+at,CustomTrainer.SKIN_NAMES.get(at),CustomTrainer.slim(texture)?t("zianrct.designer.arms.slim","Brazos delgados"):t("zianrct.designer.arms.wide","Brazos gruesos")));}minecraft.setScreen(new TrainerChoiceScreen(this,t("zianrct.designer.appearance","Apariencia del entrenador"),options,List.of(""+skin),1,chosen->{if(!chosen.isEmpty())skin=Integer.parseInt(chosen.getFirst());minecraft.setScreen(this);}));});
            label(t("zianrct.designer.no_medals","No exige medallas de Rassvet ni otorga nuevas medallas."),180);
            label(t("zianrct.designer.difficulty_hint","Dificultad ajusta IV/EV e IA; los niveles se editan aparte."),196);
            label(t("zianrct.designer.loot_hint","El loot se configura después en Modificar > Loot."),212);
            contentHeight=236;
        }else if(tab==1){
            label(t("zianrct.designer.species_level","Especie / nivel (1–100). Vacío = no usar esa plaza."),26);
            button(left,42,usable,t("zianrct.designer.moves_mode","Movimientos: %s (cambiar)",autoMoves?t("zianrct.designer.auto_moves","Automáticos por dificultad"):t("zianrct.designer.manual_moves","Manuales")),()->{read();autoMoves=!autoMoves;init();});
            label(autoMoves?t("zianrct.designer.changes_on_save","Los cambios se aplican al pulsar Guardar entrenador."):t("zianrct.designer.use_selectors","Usa los selectores o escribe tus ataques."),66);
            int row=84;
            for(int i=0;i<6;i++){
                int slot=i;
                EditBox s=field(left,row,usable-162,96,species[i]);EditBox l=field(left+usable-62,row,62,3,levels[i]);
                button(left+usable-156,row,88,t("zianrct.designer.pokemon_btn","Pokémon..."),()->selectSpecies(slot));
                reads.add(()->{species[slot]=s.getValue();levels[slot]=l.getValue();});
                label(autoMoves?t("zianrct.designer.preview_moves","Vista anterior; los ataques se recalculan al guardar."):t("zianrct.designer.moves_list","Movimientos (1–4, separados por coma):"),row+22);
                EditBox m=field(left,row+36,usable-94,200,moves[i]);reads.add(()->moves[slot]=m.getValue());
                m.setEditable(!autoMoves);
                if(!autoMoves)button(left+usable-88,row+36,88,t("zianrct.designer.moves_btn","Ataques..."),()->selectMoves(slot));
                row+=64;
            }
            contentHeight=row;
        }else if(tab==2){
            label(t("zianrct.designer.on_start","Al iniciar el combate:"),26);EditBox s=field(left,42,usable,256,start);reads.add(()->start=s.getValue());
            label(t("zianrct.designer.on_win","Cuando gana el jugador:"),68);EditBox a=field(left,84,usable,256,win);reads.add(()->win=a.getValue());
            label(t("zianrct.designer.on_loss","Cuando pierde el jugador:"),110);EditBox b=field(left,126,usable,256,loss);reads.add(()->loss=b.getValue());
            label(t("zianrct.designer.saved_as_dialogues","Se guardan como diálogos propios de este ID."),154);contentHeight=180;
        }else{
            button(left,28,usable,t("zianrct.designer.trial","Prueba legendaria: %s (cambiar)",trialEnabled?t("zianrct.designer.on","Activada"):t("zianrct.designer.off","Desactivada")),()->{read();trialEnabled=!trialEnabled;init();});
            label(t("zianrct.designer.boss_id","ID del jefe que desbloquea este NPC (dificultad JEFE):"),56);
            EditBox boss=field(left,72,usable,64,trialBoss);reads.add(()->trialBoss=boss.getValue());
            label(t("zianrct.designer.shiny","Probabilidad shiny: 1 entre (512 por defecto):"),100);
            EditBox chance=field(left,116,usable,7,shinyDenominator);reads.add(()->shinyDenominator=chance.getValue());
            label(t("zianrct.designer.trial_team","Equipo: un legendario/singular, batalla Individual."),145);
            label(t("zianrct.designer.trial_reward","Al vencer: Pokémon al PC, IV aleatorios de 25 a 30."),163);
            label(t("zianrct.designer.trial_unique","Único por jugador. Perder permite volver a intentar."),181);
            label(t("zianrct.designer.trial_boss_first","Crea primero el jefe; su loot se configura por separado."),199);
            contentHeight=226;
        }
        save=addRenderableWidget(gold(x+12,y+h-25,160,t("zianrct.designer.save","Guardar entrenador"),()->submit()));
        back=addRenderableWidget(gold(x+w-112,y+h-25,100,t("zianrct.designer.back","Volver a lista"),()->PacketDistributor.sendToServer(new NpcEditorAction(state.nonce(),"list","",""))));
        position();
    }
    private void submit(){
        read();
        try{
            List<CustomTrainer.Member> team=new ArrayList<>();
            for(int i=0;i<6;i++)if(!species[i].isBlank())team.add(new CustomTrainer.Member(species[i].trim().toLowerCase(Locale.ROOT),
                    Integer.parseInt(levels[i].trim()),autoMoves?List.of("tackle"):Arrays.stream(moves[i].split(",")).map(String::trim).map(s->s.toLowerCase(Locale.ROOT)).filter(s->!s.isEmpty()).toList()));
            var trial=trialEnabled?new CustomTrainer.Trial(trialBoss.trim(),Integer.parseInt(shinyDenominator.trim())):null;
            var d=new CustomTrainer(id.trim(),name.trim(),difficulty,format,skin,team,start,win,loss,autoMoves,trial);
            d=AutomaticTrainerMoves.prepare(d);
            PacketDistributor.sendToServer(new CustomTrainerSave(state.nonce(),new Gson().toJson(d)));
        }catch(RuntimeException e){error=e.getMessage()==null?t("zianrct.designer.check_definition","Revisa la definición"):e.getMessage();}
    }
    private void selectSpecies(int slot){
        read();
        var choices=com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getImplemented().stream()
            .map(s->new TrainerChoiceScreen.Choice(s.getResourceIdentifier().toString(),s.getTranslatedName().getString(),t("zianrct.designer.dex","Pokédex #%s",s.getNationalPokedexNumber())))
            .sorted(Comparator.comparing(TrainerChoiceScreen.Choice::name)).toList();
        String current=species[slot].contains(":")?species[slot]:"cobblemon:"+species[slot];
        minecraft.setScreen(new TrainerChoiceScreen(this,t("zianrct.designer.select_pokemon","Seleccionar Pokémon · plaza %s",slot+1),choices,List.of(current),1,ids->{
            if(ids.isEmpty()){species[slot]="";return;}
            String chosen=ids.getFirst();
            if(!chosen.equals(current)){
                species[slot]=chosen;
                var s=com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByIdentifier(net.minecraft.resources.ResourceLocation.parse(chosen));
                int level;try{level=Integer.parseInt(levels[slot]);}catch(NumberFormatException e){level=25;}
                moves[slot]=s.getStandardForm().getMoves().getLevelUpMovesUpTo(Math.max(1,Math.min(100,level))).stream()
                    .map(com.cobblemon.mod.common.api.moves.MoveTemplate::getName).sorted().limit(4).collect(java.util.stream.Collectors.joining(","));
            }
        }));
    }
    private void selectMoves(int slot){
        read();
        var id=net.minecraft.resources.ResourceLocation.tryParse(species[slot].contains(":")?species[slot]:"cobblemon:"+species[slot]);
        var s=id==null?null:com.cobblemon.mod.common.api.pokemon.PokemonSpecies.getByIdentifier(id);
        if(s==null){error=t("zianrct.designer.pick_valid","Selecciona un Pokémon válido primero.");return;}
        var choices=s.getStandardForm().getMoves().getAllLegalMoves().stream()
            .map(m->new TrainerChoiceScreen.Choice(m.getName(),m.getDisplayName().getString(),m.getDescription().getString()+" · Potencia: "+m.getPower()+" · Precisión: "+m.getAccuracy()+" · PP: "+m.getPp()))
            .sorted(Comparator.comparing(TrainerChoiceScreen.Choice::name)).toList();
        var legal=choices.stream().map(TrainerChoiceScreen.Choice::id).collect(java.util.stream.Collectors.toSet());
        var initial=Arrays.stream(moves[slot].split(",")).map(String::trim).filter(legal::contains).distinct().limit(4).toList();
        minecraft.setScreen(new TrainerChoiceScreen(this,t("zianrct.designer.moves_of","Ataques de %s (incluye MT / tutor)",s.getTranslatedName().getString()),choices,initial,4,ids->{
            if(ids.isEmpty()){error=t("zianrct.designer.needs_move","El Pokémon necesita al menos un ataque.");return;}
            moves[slot]=String.join(",",ids);
        }));
    }
    private EditBox field(int left,int row,int size,int max,String value){
        var box=new EditBox(font,left,y+32+row,size,18,Component.translatableWithFallback("ui.zianrct.f6a1062a638b", "Dato del entrenador"));box.setMaxLength(max);box.setValue(value);
        addRenderableWidget(box);rows.put(box,row);return box;
    }
    private void label(String text,int row){labels.add(new Label(text,row));}
    private Button gold(int bx,int by,int size,String text,Runnable action){
        return new Button(bx,by,size,18,Component.literal(text),b->action.run(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                g.fill(getX(),getY(),getX()+getWidth(),getY()+18,0xFFF2C14E);
                g.fill(getX()+1,getY()+1,getX()+getWidth()-1,getY()+17,isHoveredOrFocused()?0xFF3A3326:0xFF242424);
                g.drawCenteredString(font,getMessage(),getX()+getWidth()/2,getY()+5,0xFFF7E2AC);
            }
        };
    }
    private void button(int left,int row,int size,String text,Runnable action){var b=addRenderableWidget(gold(left,y+32+row,size,text,action));rows.put(b,row);}
    private void position(){
        scroll=Math.max(0,Math.min(scroll,Math.max(0,contentHeight-(h-68))));
        rows.forEach((widget,row)->{widget.setY(y+32+row-scroll);widget.visible=widget.getY()+widget.getHeight()>y+30 && widget.getY()<y+h-36;});
    }
    @Override protected void renderCanvas(GuiGraphics g,int mx,int my,float p){
        g.fill(0,0,width,height,0x90000000);g.fill(x,y,x+w,y+h,0xF0181818);g.fill(x,y,x+w,y+2,0xFFF2C14E);
        g.drawCenteredString(font,title,width/2,y+12,0xFFF2C14E);
        canvasScissor(g,x+6,y+30,x+w-6,y+h-36);
        for(var label:labels)g.drawString(font,font.plainSubstrByWidth(label.text,w-24),x+12,y+32+label.row-scroll,0xB8B8B8,false);
        for(var r:renderables)if(r!=save && r!=back)r.render(g,mx,my,p);
        g.disableScissor();save.render(g,mx,my,p);back.render(g,mx,my,p);
        if(!error.isBlank())g.drawString(font,font.plainSubstrByWidth(error,w-24),x+12,y+h-35,0xF2C14E,false);
    }
    @Override public boolean canvasMouseScrolled(double mx,double my,double horizontal,double vertical){scroll-=(int)(vertical*24);position();return true;}
    @Override public boolean canvasMouseClicked(double mx,double my,int button){
        if(save.isMouseOver(mx,my)||back.isMouseOver(mx,my))return super.canvasMouseClicked(mx,my,button);
        if(my<y+30||my>=y+h-36||mx<x||mx>=x+w)return false;
        return super.canvasMouseClicked(mx,my,button);
    }
    @Override public boolean isPauseScreen(){return false;}
}
