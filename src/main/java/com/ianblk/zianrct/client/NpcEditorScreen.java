package com.ianblk.zianrct.client;
import com.ianblk.zianrct.npc.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class NpcEditorScreen extends Screen {
    private final NpcEditorState state;
    private final Map<AbstractWidget,Integer> base = new LinkedHashMap<>();
    private final List<TextLine> labels = new ArrayList<>();
    private int x,y,w,h,scroll,contentHeight;
    private Button close;
    private EditBox trainer,currency,coins;
    private static boolean lootTab;
    private long lastSend;
    private String confirm = "";
    private record TextLine(String text, int row, int color) {}
    public NpcEditorScreen(NpcEditorState state) { super(Component.literal("Zian RCT · Editor de NPC")); this.state = state; }
    @Override protected void init() {
        base.clear(); labels.clear(); clearWidgets();
        w = Math.min(460, width - 16); h = Math.min(270, height - 16);
        x = (width-w)/2; y=(height-h)/2;
        int left=x+12, usable=w-24;
        button(left,0,usable/2-3,"NPC",()->{lootTab=false;scroll=0;init();});
        button(left+usable/2+3,0,usable/2-3,"Loot",()->{lootTab=true;scroll=0;init();});
        if (!lootTab) {
            label(state.npc().isEmpty() ? "Plantilla: selecciona un ID cargado en RCT." : "NPC cercano seleccionado: " + state.trainer(),24,0xF7E2AC);
            trainer = field(left,40,usable-80,state.trainer());
            button(left+usable-74,40,74,"Buscar",()->send("search",trainer.getValue(),""));
            button(left,64,usable/2-3,"Usar ID",()->send("select",trainer.getValue(),""));
            button(left+usable/2+3,64,usable/2-3,"NPC cercano",()->send("nearby","",""));
            int row=90;
            label("Coincidencias (busca para filtrar):",row,0xB8B8B8); row+=16;
            for(String id:state.matches()) {
                String label=font.plainSubstrByWidth(id,usable-12);
                button(left,row,usable,label,()->send("select",id,"")); row+=22;
            }
            if(state.matches().isEmpty()){label("Sin coincidencias.",row,0xB8B8B8);row+=18;}
            button(left,row,usable,"Hacer aparecer delante de mí",()->send("spawn","","")); row+=24;
            button(left,row,usable,"Permanente: "+(state.persistent()?"Sí":"No"),()->send("persistent","","")); row+=24;
            button(left,row,usable,"Movimiento: "+(state.frozen()?"Bloqueado":"Permitido"),()->send("movement","",""));row+=24;
            label("Permanencia y movimiento son por NPC.",row,0xB8B8B8);row+=18;
            label("No se modifica su equipo ni su progresión.",row,0xB8B8B8);row+=20;
            contentHeight=row;
        } else {
            label("Loot de: "+state.trainer(),24,0xF7E2AC);
            label("Compartido por ID · Una vez por jugador",40,0xB8B8B8);
            label("Moneda AVECOINS / cantidad",62,0xB8B8B8);
            currency=field(left,78,usable-72,state.currency().isEmpty()?"avecoins:coppercoin":state.currency());
            coins=field(left+usable-66,78,66,Long.toString(state.coins()));
            button(left,102,usable,"Guardar monedas (0 = quitar)",()->send("money",currency.getValue(),coins.getValue()));
            int row=128;
            label("Objetos guardados: "+state.rewards().size(),row,0xF7E2AC);row+=18;
            for(String item:state.rewards()){label(item,row,0xB8B8B8);row+=16;}
            button(left,row,usable,"Añadir stack de mi mano principal",()->send("add_item","",""));row+=24;
            button(left,row,usable,"Vaciar objetos",()->destructive("clear_items"));row+=24;
            button(left,row,usable,"Quitar loot del entrenador",()->destructive("remove_reward"));row+=26;
            label("Editar no cambia premios ya reservados.",row,0xB8B8B8);row+=20;
            contentHeight=row;
        }
        close=addRenderableWidget(new Button(width/2-45,y+h-25,90,18,Component.literal("Cerrar"),b->onClose(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float p){drawButton(this,g);}
        });
        position();
    }
    private EditBox field(int left,int row,int size,String value){
        EditBox box=new EditBox(font,left,y+32+row,size,18,Component.literal("Dato del entrenador"));
        box.setMaxLength(128);box.setValue(value);addRenderableWidget(box);base.put(box,row);return box;
    }
    private void button(int left,int row,int size,String text,Runnable action){
        Button button=new Button(left,y+32+row,size,18,Component.literal(text),b->action.run(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float p){drawButton(this,g);}
        };
        addRenderableWidget(button);base.put(button,row);
    }
    private void drawButton(Button button,GuiGraphics g){
        int bx=button.getX(),by=button.getY();
        g.fill(bx,by,bx+button.getWidth(),by+button.getHeight(),0xFFF2C14E);
        g.fill(bx+1,by+1,bx+button.getWidth()-1,by+button.getHeight()-1,button.isHoveredOrFocused()?0xFF3A3326:0xFF242424);
        g.drawCenteredString(font,button.getMessage(),bx+button.getWidth()/2,by+5,0xFFF7E2AC);
    }
    private void label(String text,int row,int color){labels.add(new TextLine(text,row,color));}
    private void position(){
        int viewport=h-66;
        scroll=Math.max(0,Math.min(scroll,Math.max(0,contentHeight-viewport)));
        base.forEach((widget,row)->{widget.setY(y+32+row-scroll);widget.visible=widget.getY()+widget.getHeight()>y+30&&widget.getY()<y+h-36;});
    }
    private void destructive(String action){
        if(!confirm.equals(action)){
            confirm=action;
            if(minecraft.player!=null)minecraft.player.sendSystemMessage(Component.literal("Pulsa de nuevo para confirmar. Los premios reservados se conservan."));
            return;
        }
        send(action,"","");
    }
    private void send(String action,String value,String extra){
        long now=System.nanoTime();
        if(now-lastSend<600_000_000L)return;
        lastSend=now;
        try{PacketDistributor.sendToServer(new NpcEditorAction(state.nonce(),action,value,extra));}
        catch(IllegalArgumentException e){if(minecraft.player!=null)minecraft.player.sendSystemMessage(Component.literal("Revisa el ID, la moneda y el importe."));}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0x90000000);
        g.fill(x,y,x+w,y+h,0xF0181818);g.fill(x,y,x+w,y+2,0xFFF2C14E);
        g.drawCenteredString(font,title,width/2,y+12,0xFFF2C14E);
        g.enableScissor(x+6,y+30,x+w-6,y+h-36);
        for(var label:labels)g.drawString(font,font.plainSubstrByWidth(label.text,w-24),x+12,y+32+label.row-scroll,label.color,false);
        for(var renderable:renderables)if(renderable!=close)renderable.render(g,mx,my,partial);
        g.disableScissor();
        close.render(g,mx,my,partial);
        if(!state.notice().isBlank()&&minecraft.player!=null){
            g.drawString(font,font.plainSubstrByWidth(state.notice(),w-24),x+12,y+h-35,0xF2C14E,false);
        }
        if(contentHeight>h-66)g.drawString(font,"↕",x+w-12,y+h-50,0xF2C14E,false);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){
        scroll-=(int)(vertical*24);position();return true;
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(close.isMouseOver(mx,my))return super.mouseClicked(mx,my,button);
        if(my<y+30||my>=y+h-36||mx<x||mx>=x+w)return false;
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean isPauseScreen(){return false;}
}
