package com.ianblk.zianrct.reward;
import com.ianblk.zianrct.creator.CustomTrainer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class SkinLayoutTest {
 @Test void appendedSkinsPreserveOldIndicesAndSelectBothModels()throws Exception{
  assertTrue(CustomTrainer.SKINS.get(0).endsWith("/arctic_explorer.png"));assertTrue(CustomTrainer.SKINS.get(5).endsWith("/amber_captain.png"));
  for(int i=6;i<CustomTrainer.SKINS.size();i++){var path=CustomTrainer.SKINS.get(i);try(var in=getClass().getResourceAsStream("/assets/"+path.replace(':','/'))){assertNotNull(in,path);var image=javax.imageio.ImageIO.read(in);boolean slim=true;for(int y=0;y<12;y++)for(int x=0;x<2;x++)slim&=(image.getRGB(54+x,20+y)>>>24)==0&&(image.getRGB(46+x,52+y)>>>24)==0;assertEquals(slim,CustomTrainer.slim(path),path);}}
 }
}
