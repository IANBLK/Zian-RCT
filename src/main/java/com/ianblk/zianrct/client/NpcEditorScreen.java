package com.ianblk.zianrct.client;
import static com.ianblk.zianrct.Messages.t;
import com.ianblk.zianrct.npc.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class NpcEditorScreen extends ResponsiveScreen {
    private final NpcEditorState state;
    private final Map<AbstractWidget,Integer> base = new LinkedHashMap<>();
    private final List<TextLine> labels = new ArrayList<>();
    private int x,y,w,h,scroll,contentHeight;
    private Button close;
    private EditBox trainer,currency,coins,cooldown;
    private boolean repeatChoice;
    private static boolean lootTab;
    private long lastSend;
    private String confirm = "";
    private record TextLine(String text, int row, int color, int maxWidth, int centerX) {}
    public NpcEditorScreen(NpcEditorState state) { super(Component.translatableWithFallback("ui.zianrct.ff1abce411eb", "Zian RCT · Gestor de NPC")); this.state = state;
        repeatChoice=state.rewardMode().equals("REPEAT");if(state.mode()!=NpcEditorState.Mode.EDIT)lootTab=false; }
    @Override protected void init() {fitViewport(600,360);
        base.clear(); labels.clear(); clearWidgets();
        w=Math.min(520,width-16);h=Math.min(290,height-16);x=(width-w)/2;y=(height-h)/2;
        int left=x+12,usable=w-24,row;
        if(state.mode()==NpcEditorState.Mode.LIST){
            button(left,0,usable/2-3,t("zianrct.npc.create","Crear NPC"),()->send("create","",""));
            button(left+usable/2+3,0,usable/2-3,t("zianrct.npc.refresh_list","Actualizar lista"),()->send("list","",""));
            label(t("zianrct.npc.loaded_hint","NPC cargados en tu dimensión · más cercanos primero"),26,0xB8B8B8);
            row=44;
            for(var npc:state.npcs()){
                label(display(npc.trainer())+" ["+npc.uuid().substring(0,8)+"]",row,0xF7E2AC,usable-156);
                label(npc.x()+" "+npc.y()+" "+npc.z()+" · "+npc.distance()+" m",row+12,0xB8B8B8,usable-156);
                button(left+usable-148,row+3,74,t("zianrct.npc.modify","Modificar"),()->send("select_npc",npc.uuid(),""));
                button(left+usable-68,row+3,68,t("zianrct.npc.delete","Eliminar"),()->send("select_delete",npc.uuid(),""));
                row+=34;
            }
            if(state.npcs().isEmpty()){label(t("zianrct.npc.none_loaded","No hay NPC cargados. Acércate a su zona o crea uno."),row,0xB8B8B8);row+=22;}
            row=pages(left,usable,row);
            contentHeight=row+4;
        }else if(state.mode()==NpcEditorState.Mode.CREATE){
            button(left,0,usable,t("zianrct.npc.back_to_list","Volver a la lista"),()->send("list","",""));
            button(left,24,usable,t("zianrct.npc.define_from_scratch","Definir entrenador desde cero"),()->send("designer","",""));
            if(state.confirmation().equals("spawn")){
                label(t("zianrct.npc.confirm_create_of","Confirmar creación de: %s",state.trainer()),48,0xF7E2AC);
                label(t("zianrct.npc.will_appear","Aparecerá delante de ti, permanente y fijo."),66,0xB8B8B8);
                button(left,90,usable/2-3,t("zianrct.npc.confirm_create","Confirmar creación"),()->send("spawn","",""));
                button(left+usable/2+3,90,usable/2-3,t("zianrct.common.cancel","Cancelar"),()->send("cancel","",""));
                contentHeight=118;
            }else{
                trainer=field(left,48,usable-80,state.query());
                button(left+usable-74,48,74,t("zianrct.npc.search","Buscar"),()->send("search",trainer.getValue(),""));
                label(state.trainer().isEmpty()?t("zianrct.npc.select_trainer","Selecciona un entrenador de la lista."):t("zianrct.npc.selected","Seleccionado: %s",state.trainer()),72,0xF7E2AC);
                button(left,90,usable,t("zianrct.npc.create_selected","Crear el seleccionado..."),()->send("spawn_prompt","","")).active=!state.trainer().isEmpty();
                row=116;
                for(String id:state.matches()){
                    button(left,row,usable,font.plainSubstrByWidth(id,usable-12),()->send("choose_template",id,""));row+=22;
                }
                if(state.matches().isEmpty()){label(t("zianrct.npc.no_matches","Sin coincidencias."),row,0xB8B8B8);row+=18;}
                contentHeight=pages(left,usable,row)+4;
            }
        }else{
            button(left,0,usable,t("zianrct.npc.back_to_list","Volver a la lista"),()->send("list","",""));
            label(t("zianrct.npc.modify_of","Modificar: %s [%s]",display(state.trainer()),state.npc().substring(0,8)),26,0xF7E2AC);
            if(state.confirmation().equals("delete")){
                label(t("zianrct.npc.delete_question","Eliminar este NPC del mundo?"),48,0xF7E2AC);
                label(t("zianrct.npc.keeps_medals","Las medallas y premios de jugadores se conservan."),66,0xB8B8B8);
                button(left,90,usable/2-3,t("zianrct.npc.confirm_delete","Confirmar eliminación"),()->send("delete","",""));
                button(left+usable/2+3,90,usable/2-3,t("zianrct.common.cancel","Cancelar"),()->send("cancel","",""));
                contentHeight=118;
            }else{
                button(left,44,usable/2-3,"NPC",()->{lootTab=false;scroll=0;init();});
                button(left+usable/2+3,44,usable/2-3,"Loot",()->{lootTab=true;scroll=0;init();});
                if(!lootTab){
                    button(left,72,usable,t("zianrct.npc.move_here","Mover a mi bloque y dirección"),()->send("move","",""));
                    button(left,96,usable,t("zianrct.npc.persistent","Permanente: %s",t(state.persistent()?"zianrct.common.yes":"zianrct.common.no",state.persistent()?"Sí":"No")),()->send("persistent","",""));
                    button(left,120,usable,t("zianrct.npc.movement","Movimiento: %s",t(state.frozen()?"zianrct.npc.movement.locked":"zianrct.npc.movement.allowed",state.frozen()?"Bloqueado":"Permitido")),()->send("movement","",""));
                    String format=state.battleFormat().equals("GEN_9_DOUBLES")?t("zianrct.format.doubles","Doble"):state.battleFormat().equals("GEN_9_SINGLES")?t("zianrct.format.singles","Individual"):state.battleFormat();
                    button(left,144,usable,t("zianrct.npc.battle_change","Batalla: %s (cambiar)",format),()->send("format",state.battleFormat().equals("GEN_9_DOUBLES")?"GEN_9_SINGLES":"GEN_9_DOUBLES",""));
                    label(t("zianrct.npc.format_shared","El formato se comparte por ID de entrenador."),168,0xB8B8B8);
                    button(left,190,usable,t("zianrct.npc.delete_this","Eliminar este NPC..."),()->send("delete_prompt","",""));
                    if(state.trainer().startsWith("zian_custom_"))button(left,220,usable,t("zianrct.npc.edit_custom","Editar definición propia"),()->send("designer","",""));
                    contentHeight=state.trainer().startsWith("zian_custom_")?248:218;
                }else{
                    label(t("zianrct.npc.loot_hint","Loot compartido por ID · Espera por jugador"),72,0xB8B8B8);
                    button(left,92,usable,t("zianrct.npc.reward_type","Tipo de premio: %s (cambiar)",t(repeatChoice?"zianrct.npc.reward.repeat":"zianrct.npc.reward.unique",repeatChoice?"Repetible":"Único")),()->{repeatChoice=!repeatChoice;init();});
                    label(t("zianrct.npc.cooldown_minutes","Espera en minutos (24 h = 1440):"),116,0xB8B8B8);
                    cooldown=field(left,132,usable-100,Long.toString(state.cooldownMinutes()==0?1440:state.cooldownMinutes()));
                    button(left+usable-94,132,94,t("zianrct.npc.save_type","Guardar tipo"),()->send("policy",repeatChoice?"REPEAT":"UNIQUE",repeatChoice?cooldown.getValue():"0"));
                    label(t("zianrct.npc.for_you","Para ti: %s",state.nextReward()),156,0xF7E2AC);
                    button(left,174,usable,t("zianrct.npc.refresh_cooldown","Actualizar mi espera"),()->send("refresh","",""));
                    label(t("zianrct.npc.currency_amount","Moneda AVECOINS / cantidad"),198,0xB8B8B8);
                    currency=field(left,214,usable-72,state.currency().isEmpty()?"avecoins:coppercoin":state.currency());
                    coins=field(left+usable-66,214,66,Long.toString(state.coins()));
                    button(left,238,usable,t("zianrct.npc.save_coins","Guardar monedas (0 = quitar)"),()->send("money",currency.getValue(),coins.getValue()));
                    row=264;
                    label(t("zianrct.npc.items_saved","Objetos guardados: %s",state.rewards().size()),row,0xF7E2AC);row+=18;
                    for(String item:state.rewards()){label(item,row,0xB8B8B8);row+=16;}
                    button(left,row,usable,t("zianrct.npc.add_stack","Añadir stack de mi mano principal"),()->send("add_item","",""));row+=24;
                    button(left,row,usable,t("zianrct.npc.clear_items","Vaciar objetos"),()->destructive("clear_items"));row+=24;
                    button(left,row,usable,t("zianrct.npc.remove_loot","Quitar loot del entrenador"),()->destructive("remove_reward"));row+=26;
                    label(t("zianrct.npc.edit_keeps_reserved","Editar no cambia premios ya reservados."),row,0xB8B8B8);contentHeight=row+22;
                }
            }
        }
        close=addRenderableWidget(new Button(width/2-45,y+h-25,90,18,Component.translatableWithFallback("ui.zianrct.4b0816bbd5e9", "Cerrar"),b->onClose(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float p){drawButton(this,g);}
        });
        position();
    }
    private String display(String id){
        String simple=id.replace("rassvet_leader_","").replace("rassvet_master_","");
        return simple.isEmpty()?id:Character.toUpperCase(simple.charAt(0))+simple.substring(1);
    }
    private int pages(int left,int usable,int row){
        if(state.pages()>1){
            button(left,row,60,"<",()->send("previous","","")).active=state.page()>0;
            centered(t("zianrct.npc.page","Página %s / %s",state.page()+1,state.pages()),left+60,left+usable-60,row+5);
            button(left+usable-60,row,60,">",()->send("next","","")).active=state.page()<state.pages()-1;
            return row+26;
        }
        return row;
    }
    private EditBox field(int left,int row,int size,String value){
        EditBox box=new EditBox(font,left,y+32+row,size,18,Component.translatableWithFallback("ui.zianrct.f6a1062a638b", "Dato del entrenador"));
        box.setMaxLength(128);box.setValue(value);addRenderableWidget(box);base.put(box,row);return box;
    }
    private Button button(int left,int row,int size,String text,Runnable action){
        Button button=new Button(left,y+32+row,size,18,Component.literal(text),b->action.run(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float p){drawButton(this,g);}
        };
        addRenderableWidget(button);base.put(button,row);return button;
    }
    private void drawButton(Button button,GuiGraphics g){
        int bx=button.getX(),by=button.getY();
        g.fill(bx,by,bx+button.getWidth(),by+button.getHeight(),0xFFF2C14E);
        g.fill(bx+1,by+1,bx+button.getWidth()-1,by+button.getHeight()-1,button.isHoveredOrFocused()?0xFF3A3326:0xFF242424);
        g.drawCenteredString(font,button.getMessage(),bx+button.getWidth()/2,by+5,button.active?0xFFF7E2AC:0xFF888888);
    }
    private void label(String text,int row,int color){label(text,row,color,w-24);}
    private void label(String text,int row,int color,int maxWidth){labels.add(new TextLine(text,row,color,maxWidth,-1));}
    private void centered(String text,int from,int to,int row){labels.add(new TextLine(text,row,0xB8B8B8,to-from,(from+to)/2));}
    private void position(){
        int viewport=h-66;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,contentHeight-viewport)));
        base.forEach((widget,row)->{widget.setY(y+32+row-scroll);widget.visible=widget.getY()+widget.getHeight()>y+30&&widget.getY()<y+h-36;});
    }
    private void destructive(String action){
        if(!confirm.equals(action)){
            confirm=action;
            if(minecraft.player!=null)minecraft.player.sendSystemMessage(com.ianblk.zianrct.Messages.format(Component.translatableWithFallback("ui.zianrct.e6e18767d0fb", "Pulsa de nuevo para confirmar. Los premios reservados se conservan.")));
            return;
        }
        send(action,"","");
    }
    private void send(String action,String value,String extra){
        long now=System.nanoTime();
        if(now-lastSend<600_000_000L)return;
        lastSend=now;
        try{PacketDistributor.sendToServer(new NpcEditorAction(state.nonce(),action,value,extra));}
        catch(IllegalArgumentException e){if(minecraft.player!=null)minecraft.player.sendSystemMessage(com.ianblk.zianrct.Messages.format(Component.translatableWithFallback("ui.zianrct.757a90001ed3", "Revisa el ID, la moneda y el importe.")));}
    }
    @Override protected void renderCanvas(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0x90000000);
        g.fill(x,y,x+w,y+h,0xF0181818);g.fill(x,y,x+w,y+2,0xFFF2C14E);
        g.drawCenteredString(font,title,width/2,y+12,0xFFF2C14E);
        canvasScissor(g,x+6,y+30,x+w-6,y+h-36);
        for(var label:labels){var shown=font.plainSubstrByWidth(label.text,label.maxWidth);if(label.centerX>=0)g.drawCenteredString(font,shown,label.centerX,y+32+label.row-scroll,label.color);else g.drawString(font,shown,x+12,y+32+label.row-scroll,label.color,false);}
        for(var renderable:renderables)if(renderable!=close)renderable.render(g,mx,my,partial);
        g.disableScissor();
        close.render(g,mx,my,partial);
        if(!state.notice().isBlank()&&minecraft.player!=null){
            g.drawString(font,font.plainSubstrByWidth(com.ianblk.zianrct.Messages.decode(state.notice()).getString(),w-24),x+12,y+h-35,0xF2C14E,false);
        }
        if(contentHeight>h-66)g.drawString(font,"↕",x+w-12,y+h-50,0xF2C14E,false);
    }
    @Override public boolean canvasMouseScrolled(double mx,double my,double horizontal,double vertical){
        scroll-=(int)(vertical*24);position();return true;
    }
    @Override public boolean canvasMouseClicked(double mx,double my,int button){
        if(close.isMouseOver(mx,my))return super.canvasMouseClicked(mx,my,button);
        if(my<y+30||my>=y+h-36||mx<x||mx>=x+w)return false;
        return super.canvasMouseClicked(mx,my,button);
    }
    @Override public boolean isPauseScreen(){return false;}
}
