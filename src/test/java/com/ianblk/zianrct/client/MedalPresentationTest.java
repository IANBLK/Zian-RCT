package com.ianblk.zianrct.client;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MedalPresentationTest {
    @Test
    void panelFitsCommonGuiScalesAndEveryMedalHasAPage() {
        for (int[] size : List.of(new int[]{960, 540}, new int[]{640, 360}, new int[]{480, 270}, new int[]{320, 180})) {
            for (int count : List.of(0, 10, 48)) {
                var layout = MedalCaseLayout.calculate(size[0], size[1], count);
                assertTrue(layout.x() >= 0 && layout.y() >= 0);
                assertTrue(layout.x() + layout.width() <= size[0]);
                assertTrue(layout.y() + layout.height() <= size[1]);
                assertTrue(layout.pages() * layout.perPage() >= count);
                assertEquals(Math.max(1, (count + layout.perPage() - 1) / layout.perPage()), layout.pages());
            }
        }
        assertEquals(2, MedalCaseLayout.calculate(640, 360, 10).rows());
        assertTrue(MedalCaseLayout.calculate(640, 360, 10).height() < 240);
        assertTrue(MedalCaseLayout.calculate(320, 180, 10).pages() > 1);
    }

    @Test
    void allApprovedMedalsHaveDistinctValidRegionsAndCustomTexturesRemainUntouched() throws Exception {
        try (var stream = getClass().getResourceAsStream("/assets/zianrct/textures/gui/medals/approved_atlas.png")) {
            assertNotNull(stream);
            var image = ImageIO.read(stream);
            assertEquals(MedalArtwork.WIDTH, image.getWidth());
            assertEquals(MedalArtwork.HEIGHT, image.getHeight());
            var regions = new java.util.HashSet<MedalArtwork.Region>();
            for (String id : List.of("novato", "ferrum", "aquila", "voltar", "engranaje", "bruma", "cognitus", "forjax", "glacius", "aurelia")) {
                var region = MedalArtwork.region(id, "zianrct:textures/gui/medals/" + id + ".png");
                assertNotNull(region);
                assertTrue(regions.add(region));
                assertTrue(region.x() >= 0 && region.y() >= 0);
                assertTrue(region.x() + region.width() <= image.getWidth());
                assertTrue(region.y() + region.height() <= image.getHeight());
                assertNull(MedalArtwork.region(id, "custom:textures/" + id + ".png"));
            }
        }
        assertNull(MedalArtwork.region("custom", "custom:badge.png"));
    }
}
