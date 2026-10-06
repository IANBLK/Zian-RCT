package com.ianblk.zianrct.client;

import java.util.List;

/** Source regions from the approved render, preserving its original pixels. */
public final class MedalArtwork {
    public static final int WIDTH = 1817;
    public static final int HEIGHT = 866;
    private static final List<String> IDS = List.of("novato", "ferrum", "aquila", "voltar", "engranaje",
            "bruma", "cognitus", "forjax", "glacius", "aurelia");

    private MedalArtwork() {}

    public static Region region(String id, String texture) {
        int index = IDS.indexOf(id);
        if (index < 0 || !("zianrct:textures/gui/medals/" + id + ".png").equals(texture)) return null;
        return new Region(162 + (index % 5) * 310, index < 5 ? 124 : 444, 250, 232);
    }

    public record Region(int x, int y, int width, int height) {}
}
