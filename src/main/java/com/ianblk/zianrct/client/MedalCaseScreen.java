package com.ianblk.zianrct.client;

import com.ianblk.zianrct.network.MedalClientSnapshot;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MedalCaseScreen extends Screen {
    private static final int CARD_WIDTH = 64;
    private static final int CARD_HEIGHT = 72;
    private static final int GAP = 10;
    private static final int BADGE_SIZE = 38;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public MedalCaseScreen() {
        super(Component.literal("Medallero"));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x90000000);

        int panelWidth = Math.max(0, Math.min(410, width - 24));
        int panelHeight = Math.max(0, height - 16);
        int panelX = (width - panelWidth) / 2;
        int panelY = 8;

        if (panelWidth > 0 && panelHeight > 0) {
            int panelRight = panelX + panelWidth;
            int panelBottom = panelY + panelHeight;
            graphics.fill(panelX, panelY, panelRight, panelBottom, 0xE0181818);
            graphics.fill(panelX, panelY, panelRight, panelY + 1, 0xFF555555);
            graphics.fill(panelX, panelBottom - 1, panelRight, panelBottom, 0xFF333333);
            graphics.fill(panelX, panelY, panelX + 1, panelBottom, 0xFF555555);
            graphics.fill(panelRight - 1, panelY, panelRight, panelBottom, 0xFF333333);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Screen#render invokes renderBackground. Our override above intentionally
        // replaces vanilla's blurred background, so render the base screen first
        // and then place all medal content above it.
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);

        Optional<MedalClientSnapshot> optionalSnapshot = ClientMedalState.current();
        if (optionalSnapshot.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.literal("Esperando datos del servidor..."),
                    width / 2,
                    height / 2,
                    0xA0A0A0
            );
            return;
        }

        MedalClientSnapshot snapshot = optionalSnapshot.get();
        Map<String, MedalClientSnapshot.OwnedMedalView> owned = snapshot.ownedById();
        int columns = Math.max(1, Math.min(5, Math.max(1, (width - 30 + GAP) / (CARD_WIDTH + GAP))));
        int gridWidth = columns * CARD_WIDTH + (columns - 1) * GAP;
        int startX = (width - gridWidth) / 2;
        int startY = 44;

        MedalClientSnapshot.MedalDefinitionView hovered = null;
        MedalClientSnapshot.OwnedMedalView hoveredOwned = null;

        for (int index = 0; index < snapshot.definitions().size(); index++) {
            MedalClientSnapshot.MedalDefinitionView definition = snapshot.definitions().get(index);
            if (definition == null) {
                continue;
            }
            int column = index % columns;
            int row = index / columns;
            int x = startX + column * (CARD_WIDTH + GAP);
            int y = startY + row * (CARD_HEIGHT + GAP);
            MedalClientSnapshot.OwnedMedalView record = owned.get(definition.id());
            boolean obtained = record != null;
            boolean isHovered = mouseX >= x && mouseX < x + CARD_WIDTH && mouseY >= y && mouseY < y + CARD_HEIGHT;

            drawCard(graphics, definition, obtained, isHovered, x, y);
            if (isHovered) {
                hovered = definition;
                hoveredOwned = record;
            }
        }

        int obtainedCount = owned.size();
        graphics.drawCenteredString(
                font,
                Component.literal("Perfil: " + snapshot.activeProfile() + "  •  " + obtainedCount + "/" + snapshot.definitions().size()),
                width / 2,
                height - 18,
                0xB8B8B8
        );

        if (hovered != null) {
            graphics.renderTooltip(font, tooltipFor(hovered, hoveredOwned), Optional.empty(), mouseX, mouseY);
        }
    }

    private void drawCard(
            GuiGraphics graphics,
            MedalClientSnapshot.MedalDefinitionView definition,
            boolean obtained,
            boolean hovered,
            int x,
            int y
    ) {
        int background = hovered ? 0xD0444444 : 0xC02B2B2B;
        int border = obtained ? 0xFFF2C14E : 0xFF666666;
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, background);
        graphics.fill(x, y, x + CARD_WIDTH, y + 1, border);
        graphics.fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, border);
        graphics.fill(x, y, x + 1, y + CARD_HEIGHT, border);
        graphics.fill(x + CARD_WIDTH - 1, y, x + CARD_WIDTH, y + CARD_HEIGHT, border);

        int badgeX = x + (CARD_WIDTH - BADGE_SIZE) / 2;
        int badgeY = y + 7;
        if (obtained) {
            drawObtainedBadge(graphics, definition, badgeX, badgeY);
        } else {
            drawLockedBadge(graphics, badgeX, badgeY);
        }

        String label = font.plainSubstrByWidth(definition.name(), CARD_WIDTH - 8);
        int labelX = x + (CARD_WIDTH - font.width(label)) / 2;
        graphics.drawString(font, label, labelX, y + 54, obtained ? 0xFFFFFF : 0x888888, false);
    }

    private void drawObtainedBadge(
            GuiGraphics graphics,
            MedalClientSnapshot.MedalDefinitionView definition,
            int x,
            int y
    ) {
        ResourceLocation texture = definition.texture() == null
                ? null
                : ResourceLocation.tryParse(definition.texture());
        if (texture != null && minecraft != null && minecraft.getResourceManager().getResource(texture).isPresent()) {
            graphics.blit(texture, x, y, 0.0F, 0.0F, BADGE_SIZE, BADGE_SIZE, BADGE_SIZE, BADGE_SIZE);
            return;
        }

        int color = parseColor(definition.color(), definition.id());
        graphics.fill(x + 4, y, x + BADGE_SIZE - 4, y + 8, color);
        graphics.fill(x, y + 8, x + BADGE_SIZE, y + 24, color);
        graphics.fill(x + 5, y + 24, x + BADGE_SIZE - 5, y + 31, color);
        graphics.fill(x + 11, y + 31, x + BADGE_SIZE - 11, y + BADGE_SIZE, color);
        graphics.drawCenteredString(font, "★", x + BADGE_SIZE / 2, y + 13, 0xFFFFFFFF);
    }

    private void drawLockedBadge(GuiGraphics graphics, int x, int y) {
        int dark = 0xFF4A4A4A;
        graphics.fill(x + 4, y, x + BADGE_SIZE - 4, y + 8, dark);
        graphics.fill(x, y + 8, x + BADGE_SIZE, y + 24, dark);
        graphics.fill(x + 5, y + 24, x + BADGE_SIZE - 5, y + 31, dark);
        graphics.fill(x + 11, y + 31, x + BADGE_SIZE - 11, y + BADGE_SIZE, dark);
        graphics.drawCenteredString(font, "?", x + BADGE_SIZE / 2, y + 13, 0xFF9A9A9A);
    }

    private List<Component> tooltipFor(
            MedalClientSnapshot.MedalDefinitionView definition,
            MedalClientSnapshot.OwnedMedalView owned
    ) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(definition.name()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Entrenador: " + definition.trainer()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Tope desbloqueado: " + definition.unlockCap()).withStyle(ChatFormatting.AQUA));
        if (owned == null) {
            tooltip.add(Component.literal("Estado: bloqueada").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            String date = DATE_FORMAT.format(
                    Instant.ofEpochMilli(owned.grantedAtEpochMilli()).atZone(ZoneId.systemDefault())
            );
            tooltip.add(Component.literal("Obtenida: " + date).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal("Origen: " + owned.origin()).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal(definition.description()).withStyle(ChatFormatting.WHITE));
        return tooltip;
    }

    private static int parseColor(String configured, String seed) {
        if (configured != null) {
            String value = configured.startsWith("#") ? configured.substring(1) : configured;
            if (value.length() == 6) {
                try {
                    return 0xFF000000 | Integer.parseInt(value, 16);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int hash = seed == null ? 0x7F7F7F : seed.hashCode();
        int red = 80 + Math.floorMod(hash, 140);
        int green = 80 + Math.floorMod(hash >> 8, 140);
        int blue = 80 + Math.floorMod(hash >> 16, 140);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
