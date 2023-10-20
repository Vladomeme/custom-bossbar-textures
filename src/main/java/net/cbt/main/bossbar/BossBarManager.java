package net.cbt.main.bossbar;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.IntStream;

public class BossBarManager {

    private final MinecraftClient client;
    final HashMap<String, CustomBossBar> customBossBars = new HashMap<>();
    Map<UUID, ClientBossBar> bossBars = Maps.newLinkedHashMap();

    int renderTime = 0;

    HashMap<UUID, Float> bossBarStatuses = Maps.newLinkedHashMap();
    HashMap<UUID, Boolean> alwaysUpdate = Maps.newLinkedHashMap();

    HashMap<UUID, Identifier> activeTextures = Maps.newLinkedHashMap();
    HashMap<UUID, Identifier> activeOverlays = Maps.newLinkedHashMap();
    HashMap<UUID, Integer> textureOffsets = Maps.newLinkedHashMap();
    HashMap<UUID, Integer> overlayOffsets = Maps.newLinkedHashMap();
    HashMap<UUID, Integer> textureHeights = Maps.newLinkedHashMap();
    HashMap<UUID, Integer> overlayHeights = Maps.newLinkedHashMap();

    private static final Identifier DEFAULT = new Identifier("textures/gui/bars.png");

    public BossBarManager(MinecraftClient client) {
        this.client = client;
    }

    public void render(DrawContext context) {
        if (this.bossBars.isEmpty()) {
            clearActiveTextures();
            return;
        }
        int width = context.getScaledWindowWidth();
        int height = 12;
        for (BossBar bossBar : this.bossBars.values()) {
            String name = bossBar.getName().getString();
            if (customBossBars.containsKey(name)) {
                this.renderCustomBossBar(context, width, height, customBossBars.get(name), bossBar);
                height += customBossBars.get(name).height();
            }
            else {
                this.renderDefaultBossBar(context, width, height, bossBar);
                height += 10 + this.client.textRenderer.fontHeight;
            }
        }
    }

    private void renderCustomBossBar(DrawContext context, int width, int height, CustomBossBar bossBar, BossBar source) {
        float right = (float) width / 2 - (float) bossBar.width() / 2;
        int barHalf = (bossBar.right() - bossBar.left()) / 2;
        UUID uuid = source.getUuid();

        if (bossBarStatuses.get(uuid) == null || alwaysUpdate.get(uuid) || bossBarStatuses.get(uuid) != source.getPercent()) {
            updateActiveTextures(bossBar, uuid, source.getPercent());
        }

        Identifier texture = activeTextures.get(uuid);
        Identifier overlay = activeOverlays.get(uuid);
        int textureV = textureOffsets.get(uuid);
        int overlayV = overlayOffsets.get(uuid);
        int textureHeight = textureHeights.get(uuid);
        int overlayHeight = overlayHeights.get(uuid);

        RenderSystem.enableBlend();
        context.drawTexture(texture, (int) right, height, 100,
                0, textureV, bossBar.width(), bossBar.height(), bossBar.width(), textureHeight);

        int barLength = (int) (source.getPercent() * (float) (bossBar.right() - bossBar.left()));

        if (barLength > 0) {
            switch (bossBar.type()) {
                case NORMAL -> context.drawTexture(overlay, (int) right + bossBar.left(), height, 110,
                        bossBar.left(), overlayV, barLength, bossBar.height(), bossBar.width(), overlayHeight);

                case REVERSE -> context.drawTexture(overlay, (int) right + bossBar.right() - barLength, height, 110,
                        bossBar.right() - barLength, overlayV, barLength, bossBar.height(), bossBar.width(), overlayHeight);

                case DOUBLE -> context.drawTexture(overlay, (int) right + bossBar.left() + (bossBar.right() - bossBar.left() - barLength) / 2, height, 110,
                        bossBar.left() + (float) (bossBar.right() - bossBar.left() - barLength) / 2, overlayV,
                        barLength, bossBar.height(), bossBar.width(), overlayHeight);

                case DOUBLE_REVERSE -> {
                    context.drawTexture(overlay, (int) right + bossBar.left(), height, 110,
                            bossBar.left(), overlayV, barLength / 2 + 1, bossBar.height(), bossBar.width(), overlayHeight);

                    context.drawTexture(overlay, (int) (right + bossBar.left() + barHalf + (float) (bossBar.right() - bossBar.left() - barLength) / 2) + 1, height, 110,
                            bossBar.left() + barHalf + (float) (bossBar.right() - bossBar.left() - barLength) / 2 + 1, overlayV,
                            barLength / 2, bossBar.height(), bossBar.width(), overlayHeight);
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private void updateActiveTextures(CustomBossBar bossBar, UUID uuid, float progress) {
        Float phase = getTextureKey(bossBar.phases(), progress);
        bossBarStatuses.put(uuid, progress);

        activeTextures.put(uuid, bossBar.textures().get(phase));
        activeOverlays.put(uuid, bossBar.overlays().get(phase));

        textureOffsets.put(uuid, getFrameOffset(renderTime, bossBar.textureFrames(), bossBar.height(), phase));
        overlayOffsets.put(uuid, getFrameOffset(renderTime, bossBar.overlayFrames(), bossBar.height(), phase));

        textureHeights.put(uuid, bossBar.textureFrames().get(phase) == null ? bossBar.height() :
                bossBar.height() * bossBar.textureFrames().get(phase).length);
        overlayHeights.put(uuid, bossBar.overlayFrames().get(phase) == null ? bossBar.height() :
                bossBar.height() * bossBar.overlayFrames().get(phase).length);

        if (alwaysUpdate.containsKey(uuid)) return;

        alwaysUpdate.put(uuid, !bossBar.textureFrames().isEmpty() && !bossBar.overlayFrames().isEmpty());
    }

    private void clearActiveTextures() {
        bossBarStatuses.clear();
        alwaysUpdate.clear();

        activeTextures.clear();
        activeOverlays.clear();
        textureOffsets.clear();
        overlayOffsets.clear();
        textureHeights.clear();
        overlayHeights.clear();
    }

    private float getTextureKey(List<Float> splits, Float percent) {
        for (Float split : splits) {
            if (percent * 100 < split) return split;
        }
        return 100.0f;
    }

    private int getFrameOffset(long renderTime, HashMap<Float, int[]> frames, int height, Float key) {
        int[] timings = frames.get(key);
        if (timings == null || timings.length == 0) return 0;

        int progress = (int) renderTime % IntStream.of(timings).sum();
        int counter = 0;

        for (int i = 0; i < timings.length; i++) {
            counter += timings[i];
            if (counter > progress) return i * height;
        }
        return 0;
    }

    private void renderDefaultBossBar(DrawContext context, int width, int height, BossBar bossBar) {
        this.renderDefaultBossBar(context, width / 2 - 91, height, bossBar, 182, 0);
        int progress = (int) (bossBar.getPercent() * 183.0f);
        if (progress > 0) {
            this.renderDefaultBossBar(context, width / 2 - 91, height, bossBar, progress, 5);
        }
        Text text = bossBar.getName();
        int textWidth = this.client.textRenderer.getWidth(text);
        context.drawTextWithShadow(this.client.textRenderer, text, width / 2 - textWidth / 2, height - 9, 0xFFFFFF);
    }

    private void renderDefaultBossBar(DrawContext context, int x, int y, BossBar bossBar, int width, int height) {
        context.drawTexture(DEFAULT, x, y, 0, bossBar.getColor().ordinal() * 5 * 2 + height, width, 5);
        if (bossBar.getStyle() != BossBar.Style.PROGRESS) {
            RenderSystem.enableBlend();
            context.drawTexture(DEFAULT, x, y, 0, 80 + (bossBar.getStyle().ordinal() - 1) * 5 * 2 + height, width, 5);
            RenderSystem.disableBlend();
        }
    }

    public void clear() {
        clearActiveTextures();
        customBossBars.clear();
    }

    public void setCustomBossBars(ArrayList<CustomBossBar> source) {
        for (CustomBossBar bossBar : source) {
            this.customBossBars.put(bossBar.name(), bossBar);
        }
    }

    public void setBossBars(BossBarHud source) {
        this.bossBars = source.bossBars;

    }

    public void tick() {
        this.renderTime++;
    }

    public enum Type {
        NORMAL,
        REVERSE,
        DOUBLE,
        DOUBLE_REVERSE
    }
}
