package com.ianblk.zianrct.standalone;
import com.ianblk.zianrct.creator.LeagueTrainerSkins;
import com.ianblk.zianrct.config.ZianRctConfig;
import org.junit.jupiter.api.Test;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LeagueTrainerSkinsTest {
    @Test void allTenLeagueIdsHaveDistinctUsableSkinResources() throws Exception {
        var chain=ZianRctConfig.defaults().activeProfileConfig().chain();Set<String> found=new HashSet<>();
        for(var trainer:chain){
            String skin=LeagueTrainerSkins.texture(trainer.trainer());assertNotNull(skin);assertTrue(found.add(skin));
            byte[] png=Files.readAllBytes(Path.of("src/main/resources/assets/zianrct",skin.substring("zianrct:".length())));
            assertArrayEquals(new byte[]{(byte)137,80,78,71,13,10,26,10},Arrays.copyOf(png,8));
            var header=ByteBuffer.wrap(png).order(ByteOrder.BIG_ENDIAN);assertEquals(64,header.getInt(16));assertEquals(64,header.getInt(20));
        }
        assertEquals(10,found.size());assertNull(LeagueTrainerSkins.texture("zian_custom_other"));
    }
}
