package com.ianblk.zianrct.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Local registry browser. Only the creator's final Save sends changes to the server. */
public final class TrainerChoiceScreen extends Screen {
    public record Choice(String id,String name,String detail) {}
    private final Screen parent;
    private final List<Choice> choices;
    private final LinkedHashSet<String> selected;
    private final int limit;
    private final Consumer<List<String>> accept;
    private String query="",notice="";
    private int page,x,y,w,h,pageSize;
    private List<Choice> filtered=List.of();
    private final List<Button> entries=new ArrayList<>();
    public TrainerChoiceScreen(Screen parent,String title,List<Choice> choices,List<String> selected,int limit,Consumer<List<String>> accept){
        super(Component.literal(title));this.parent=parent;this.choices=List.copyOf(choices);
        this.selected=new LinkedHashSet<>(selected);this.limit=limit;this.accept=accept;
    }
    @Override protected void init(){
        entries.clear();w=Math.min(500,width-16);h=Math.min(320,height-16);x=(width-w)/2;y=(height-h)/2;
        pageSize=Math.max(1,(h-116)/24);
        var search=addRenderableWidget(new EditBox(font,x+12,y+32,w-24,18,Component.literal("Buscar por nombre o ID")));
        search.setMaxLength(96);search.setValue(query);search.setResponder(text->{query=text;page=0;refresh();});
        for(int i=0;i<pageSize;i++){int row=i;entries.add(addRenderableWidget(button(x+12,y+59+i*24,w-24,"",()->choose(row))));}
        addRenderableWidget(button(x+12,y+h-50,60,"<",()->{page=Math.max(0,page-1);refresh();}));
        addRenderableWidget(button(x+w-72,y+h-50,60,">",()->{page=Math.min(Math.max(0,(filtered.size()-1)/pageSize),page+1);refresh();}));
        addRenderableWidget(button(x+12,y+h-26,100,"Cancelar",this::onClose));
        addRenderableWidget(button(x+w-122,y+h-26,110,"Aplicar",()->{accept.accept(List.copyOf(selected));minecraft.setScreen(parent);}));
        refresh();
    }
    private void refresh(){
        String needle=query.trim().toLowerCase(Locale.ROOT);
        filtered=choices.stream().filter(c->(c.name()+" "+c.id()).toLowerCase(Locale.ROOT).contains(needle)).toList();
        for(int i=0;i<entries.size();i++){
            int index=page*pageSize+i;Button b=entries.get(i);b.visible=index<filtered.size();
            if(b.visible){Choice c=filtered.get(index);b.setMessage(Component.literal((selected.contains(c.id())?"✓ ":"")+c.name()+" · "+c.id()));}
        }
    }
    private void choose(int row){
        var c=filtered.get(page*pageSize+row);notice="";
        if(selected.remove(c.id())){refresh();return;}
        if(limit==1)selected.clear();
        if(selected.size()>=limit){notice="Máximo "+limit+" movimientos. Desmarca uno primero.";return;}
        selected.add(c.id());refresh();
    }
    private Button button(int bx,int by,int size,String label,Runnable action){
        return new Button(bx,by,size,18,Component.literal(label),b->action.run(),s->s.get()){
            @Override protected void renderWidget(GuiGraphics g,int mx,int my,float partial){
                g.fill(getX(),getY(),getX()+getWidth(),getY()+18,0xFFF2C14E);
                g.fill(getX()+1,getY()+1,getX()+getWidth()-1,getY()+17,isHoveredOrFocused()?0xFF3A3326:0xFF242424);
                g.drawCenteredString(font,font.plainSubstrByWidth(getMessage().getString(),getWidth()-8),getX()+getWidth()/2,getY()+5,0xFFF7E2AC);
            }
        };
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        g.fill(0,0,width,height,0x90000000);g.fill(x,y,x+w,y+h,0xF0181818);g.fill(x,y,x+w,y+2,0xFFF2C14E);
        g.drawCenteredString(font,title,width/2,y+12,0xFFF2C14E);
        for(var widget:renderables)widget.render(g,mx,my,partial);
        g.drawCenteredString(font,notice.isEmpty()?"Página "+(page+1)+" / "+Math.max(1,(filtered.size()+pageSize-1)/pageSize)+" · Selección: "+selected.size():notice,width/2,y+h-43,0xFFF7E2AC);
        for(int i=0;i<entries.size();i++)if(entries.get(i).visible && entries.get(i).isMouseOver(mx,my)){
            String detail=filtered.get(page*pageSize+i).detail();
            if(!detail.isBlank())g.renderTooltip(font,Component.literal(detail),mx,my);
        }
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
