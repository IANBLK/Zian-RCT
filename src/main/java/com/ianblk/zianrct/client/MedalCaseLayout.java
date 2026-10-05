package com.ianblk.zianrct.client;

/** Compact, content-sized layout with pagination for small GUI resolutions. */
public record MedalCaseLayout(int x, int y, int width, int height, int columns, int rows,
                              int perPage, int pages) {
    public static final int CARD_WIDTH = 64;
    public static final int CARD_HEIGHT = 70;
    public static final int GAP = 6;

    public static MedalCaseLayout calculate(int screenWidth, int screenHeight, int count) {
        int columns = Math.max(1, Math.min(5, (screenWidth - 40 + GAP) / (CARD_WIDTH + GAP)));
        int availableRows = Math.max(1, (screenHeight - 16 - 84 + GAP) / (CARD_HEIGHT + GAP));
        int neededRows = Math.max(1, (Math.max(0, count) + columns - 1) / columns);
        int rows = Math.min(availableRows, neededRows);
        int perPage = columns * rows;
        int pages = Math.max(1, (Math.max(0, count) + perPage - 1) / perPage);
        int width = columns * CARD_WIDTH + (columns - 1) * GAP + 24;
        int height = rows * CARD_HEIGHT + (rows - 1) * GAP + 84;
        return new MedalCaseLayout((screenWidth - width) / 2, (screenHeight - height) / 2,
                width, height, columns, rows, perPage, pages);
    }
}
