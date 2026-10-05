package com.ianblk.zianrct.client;

import com.ianblk.zianrct.network.MedalClientSnapshot;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class MedalCaseScreen extends Screen {
    private static final int CARD_WIDTH = MedalCaseLayout.CARD_WIDTH;
    private static final int CARD_HEIGHT = MedalCaseLayout.CARD_HEIGHT;
    private static final int GAP = MedalCaseLayout.GAP;
    private static final int BADGE_SIZE = 48;
    private static final int LOCK_WIDTH = 10;
    private static final int LOCK_HEIGHT = 12;
    private static final ResourceLocation LOCK_TEXTURE = ResourceLocation.fromNamespaceAndPath("zianrct", "textures/gui/lock.png");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Map<ResourceLocation, Boolean> textureAvailability = new HashMap<>();
    private static final ResourceLocation APPROVED_ATLAS = ResourceLocation.fromNamespaceAndPath("zianrct", "textures/gui/medals/approved_atlas.png");
    private MedalCaseLayout layout;
    private int page;
    private Button previous;
    private Button next;

    public MedalCaseScreen() {
        super(Component.translatableWithFallback("screen.zianrct.medals.title", "Medallero"));
    }

    @Override
    protected void init() {
        super.init();
        textureAvailability.clear();
        layout = null;
        updateLayout();
    }

    private void updateLayout() {
        int count = ClientMedalState.current().map(s -> s.definitions().size()).orElse(0);
        MedalCaseLayout calculated = MedalCaseLayout.calculate(width, height, count);
        if (!calculated.equals(layout)) {
            layout = calculated;
            clearWidgets();
            int bottom = layout.y() + layout.height();
            addRenderableWidget(goldButton(width / 2 - 45, bottom - 27, 90,
                    Component.translatableWithFallback("screen.zianrct.medals.close", "Cerrar"), b -> onClose()));
            previous = addRenderableWidget(goldButton(layout.x() + 12, bottom - 27, 24,
                    Component.literal("<"), b -> page--));
            next = addRenderableWidget(goldButton(layout.x() + layout.width() - 36, bottom - 27, 24,
                    Component.literal(">"), b -> page++));
        }
        page = Math.max(0, Math.min(page, layout.pages() - 1));
        previous.visible = next.visible = layout.pages() > 1;
        previous.active = page > 0;
        next.active = page < layout.pages() - 1;
    }

    private Button goldButton(int x, int y, int buttonWidth, Component text, Button.OnPress action) {
        return new Button(x, y, buttonWidth, 18, text, action, supplier -> supplier.get()) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                int border = active ? 0xFFF2C14E : 0xFF666666;
                graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), border);
                graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1,
                        isHoveredOrFocused() && active ? 0xFF38332A : 0xFF242424);
                graphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + 5,
                        active ? 0xFFF7E2AC : 0xFF888888);
            }
        };
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x90000000);

        updateLayout();
        int panelWidth = layout.width();
        int panelHeight = layout.height();
        int panelX = layout.x();
        int panelY = layout.y();

        if (panelWidth > 0 && panelHeight > 0) {
            int panelRight = panelX + panelWidth;
            int panelBottom = panelY + panelHeight;
            graphics.fill(panelX, panelY, panelRight, panelBottom, 0xE0181818);
            graphics.fill(panelX, panelY, panelRight, panelY + 2, 0xFFF2C14E);
            graphics.fill(panelX, panelBottom - 1, panelRight, panelBottom, 0xFFAA8332);
            graphics.fill(panelX, panelY, panelX + 1, panelBottom, 0xFFAA8332);
            graphics.fill(panelRight - 1, panelY, panelRight, panelBottom, 0xFFAA8332);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateLayout();
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, layout.y() + 12, 0xFFF2C14E);

        Optional<MedalClientSnapshot> optionalSnapshot = ClientMedalState.current();
        if (optionalSnapshot.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.translatableWithFallback("screen.zianrct.medals.waiting", "Esperando datos del servidor..."),
                    width / 2,
                    height / 2,
                    0xA0A0A0
            );
            return;
        }

        MedalClientSnapshot snapshot = optionalSnapshot.get();
        Map<String, MedalClientSnapshot.OwnedMedalView> owned = snapshot.ownedById();
        int columns = layout.columns();
        int gridWidth = columns * CARD_WIDTH + (columns - 1) * GAP;
        int startX = (width - gridWidth) / 2;
        int startY = layout.y() + 32;

        MedalClientSnapshot.MedalDefinitionView hovered = null;
        MedalClientSnapshot.OwnedMedalView hoveredOwned = null;

        int first = page * layout.perPage();
        int end = Math.min(snapshot.definitions().size(), first + layout.perPage());
        for (int index = first; index < end; index++) {
            MedalClientSnapshot.MedalDefinitionView definition = snapshot.definitions().get(index);
            if (definition == null) continue;
            int column = (index - first) % columns;
            int row = (index - first) / columns;
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

        graphics.drawCenteredString(
                font,
                Component.translatableWithFallback(
                        "screen.zianrct.medals.profile",
                        "Perfil: %s  •  %s/%s",
                        snapshot.activeProfile(), owned.size(), snapshot.definitions().size()
                ),
                width / 2,
                layout.y() + layout.height() - 44,
                0xB8B8B8
        );

        if (hovered != null) {
            graphics.renderTooltip(font, tooltipFor(hovered, hoveredOwned), Optional.empty(), mouseX, mouseY);
        }
    }

    private void drawCard(GuiGraphics graphics, MedalClientSnapshot.MedalDefinitionView definition, boolean obtained, boolean hovered, int x, int y) {
        drawFallbackCard(graphics, obtained, hovered, x, y);

        int badgeX = x + (CARD_WIDTH - BADGE_SIZE) / 2;
        int badgeY = y + 5;
        if (obtained) drawObtainedBadge(graphics, definition, badgeX, badgeY);
        else drawLockedBadge(graphics, definition, badgeX, badgeY);

        String label = font.plainSubstrByWidth(definition.trainerName(), CARD_WIDTH - 8);
        int labelX = x + (CARD_WIDTH - font.width(label)) / 2;
        graphics.drawString(font, label, labelX, y + 58, obtained ? 0xFFF7E2AC : 0x999999, false);
    }

    private void drawFallbackCard(GuiGraphics graphics, boolean obtained, boolean hovered, int x, int y) {
        int background = hovered ? 0xD0444444 : 0xC02B2B2B;
        int border = obtained ? 0xFFF2C14E : 0xFF666666;
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, background);
        graphics.fill(x, y, x + CARD_WIDTH, y + 1, border);
        graphics.fill(x, y + CARD_HEIGHT - 1, x + CARD_WIDTH, y + CARD_HEIGHT, border);
        graphics.fill(x, y, x + 1, y + CARD_HEIGHT, border);
        graphics.fill(x + CARD_WIDTH - 1, y, x + CARD_WIDTH, y + CARD_HEIGHT, border);
    }

    private void drawObtainedBadge(GuiGraphics graphics, MedalClientSnapshot.MedalDefinitionView definition, int x, int y) {
        if (drawApprovedBadge(graphics, definition, x, y)) return;
        ResourceLocation texture = medalTexture(definition);
        if (texture != null && hasTexture(texture)) {
            graphics.blit(texture, x, y, 0.0F, 0.0F, BADGE_SIZE, BADGE_SIZE, BADGE_SIZE, BADGE_SIZE);
            return;
        }
        drawFallbackBadge(graphics, definition, x, y, false);
    }

    private void drawLockedBadge(GuiGraphics graphics, MedalClientSnapshot.MedalDefinitionView definition, int x, int y) {
        MedalArtwork.Region approved = MedalArtwork.region(definition.id(), definition.texture());
        if (approved != null && hasTexture(APPROVED_ATLAS)) {
            graphics.setColor(0.28F, 0.28F, 0.30F, 1.0F);
            drawApprovedBadge(graphics, definition, x, y);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            drawLock(graphics, x, y);
            return;
        }
        ResourceLocation texture = medalTexture(definition);
        if (texture != null && hasTexture(texture)) {
            graphics.setColor(0.18F, 0.18F, 0.20F, 1.0F);
            graphics.blit(texture, x, y, 0.0F, 0.0F, BADGE_SIZE, BADGE_SIZE, BADGE_SIZE, BADGE_SIZE);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (hasTexture(LOCK_TEXTURE)) {
                graphics.blit(LOCK_TEXTURE, x + BADGE_SIZE - LOCK_WIDTH, y, 0.0F, 0.0F, LOCK_WIDTH, LOCK_HEIGHT, LOCK_WIDTH, LOCK_HEIGHT);
            }
            return;
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        drawFallbackBadge(graphics, definition, x, y, true);
    }

    private void drawFallbackBadge(GuiGraphics graphics, MedalClientSnapshot.MedalDefinitionView definition, int x, int y, boolean locked) {
        int color = locked ? 0xFF4A4A4A : parseColor(definition.color(), definition.id());
        graphics.fill(x + 4, y, x + BADGE_SIZE - 4, y + 8, color);
        graphics.fill(x, y + 8, x + BADGE_SIZE, y + 24, color);
        graphics.fill(x + 5, y + 24, x + BADGE_SIZE - 5, y + 31, color);
        graphics.fill(x + 11, y + 31, x + BADGE_SIZE - 11, y + BADGE_SIZE, color);
        graphics.drawCenteredString(font, locked ? "?" : "★", x + BADGE_SIZE / 2, y + 13, locked ? 0xFF9A9A9A : 0xFFFFFFFF);
    }

    private boolean drawApprovedBadge(GuiGraphics graphics, MedalClientSnapshot.MedalDefinitionView definition, int x, int y) {
        MedalArtwork.Region region = MedalArtwork.region(definition.id(), definition.texture());
        if (region == null || !hasTexture(APPROVED_ATLAS)) return false;
        graphics.blit(APPROVED_ATLAS, x, y + 2, BADGE_SIZE, 45, (float) region.x(), (float) region.y(),
                region.width(), region.height(), MedalArtwork.WIDTH, MedalArtwork.HEIGHT);
        return true;
    }

    private void drawLock(GuiGraphics graphics, int x, int y) {
        if (hasTexture(LOCK_TEXTURE)) {
            graphics.blit(LOCK_TEXTURE, x + BADGE_SIZE - LOCK_WIDTH, y, 0.0F, 0.0F,
                    LOCK_WIDTH, LOCK_HEIGHT, LOCK_WIDTH, LOCK_HEIGHT);
        }
    }

    private ResourceLocation medalTexture(MedalClientSnapshot.MedalDefinitionView definition) {
        return definition.texture() == null ? null : ResourceLocation.tryParse(definition.texture());
    }

    private boolean hasTexture(ResourceLocation texture) {
        if (texture == null || minecraft == null) return false;
        return textureAvailability.computeIfAbsent(texture, this::canDecodeTexture);
    }

    private boolean canDecodeTexture(ResourceLocation texture) {
        if (minecraft == null) return false;
        var resource = minecraft.getResourceManager().getResource(texture);
        if (resource.isEmpty()) return false;
        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            return image.getWidth() > 0 && image.getHeight() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private List<Component> tooltipFor(MedalClientSnapshot.MedalDefinitionView definition, MedalClientSnapshot.OwnedMedalView owned) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(definition.name()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatableWithFallback("screen.zianrct.medals.trainer", "Entrenador: %s", definition.trainerName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatableWithFallback("screen.zianrct.medals.cap", "Tope desbloqueado: %s", definition.unlockCap()).withStyle(ChatFormatting.AQUA));
        if (owned == null) {
            tooltip.add(Component.translatableWithFallback("screen.zianrct.medals.locked", "Estado: bloqueada").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            String date = DATE_FORMAT.format(Instant.ofEpochMilli(owned.grantedAtEpochMilli()).atZone(ZoneId.systemDefault()));
            tooltip.add(Component.translatableWithFallback("screen.zianrct.medals.obtained", "Obtenida: %s", date).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatableWithFallback("screen.zianrct.medals.origin", "Origen: %s", originLabel(owned.origin())).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(Component.literal(definition.description()).withStyle(ChatFormatting.WHITE));
        return tooltip;
    }

    private Component originLabel(String origin) {
        String normalized = origin == null ? "" : origin.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BATTLE" -> Component.translatableWithFallback("screen.zianrct.medals.origin.battle", "Combate");
            case "COMMAND" -> Component.translatableWithFallback("screen.zianrct.medals.origin.command", "Comando");
            case "RECONCILED" -> Component.translatableWithFallback("screen.zianrct.medals.origin.reconciled", "Historial");
            default -> Component.literal(origin == null || origin.isBlank() ? "-" : origin);
        };
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
