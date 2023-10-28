package net.cbt.main.bossbar;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.util.math.MatrixStack;
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

    HashMap<UUID, ActiveBossBar> activeBossBars = Maps.newLinkedHashMap();
    HashMap<UUID, Boolean> alwaysUpdate = Maps.newLinkedHashMap();

    private static final Identifier DEFAULT = new Identifier("textures/gui/bars.png");

    public BossBarManager(MinecraftClient client) {
        this.client = client;
    }

    public void render(MatrixStack matrices) {
        if (this.bossBars.isEmpty()) {
            clearActiveTextures();
            return;
        }
        int width = MinecraftClient.getInstance().getWindow().getScaledWidth();
        int height = 12;
        for (BossBar bossBar : this.bossBars.values()) {
            String name = bossBar.getName().getString();
            if (customBossBars.containsKey(name)) {
                this.renderCustomBossBar(matrices, width, height, customBossBars.get(name), bossBar);
                height += 5 + customBossBars.get(name).height();
            }
            else {
                this.renderDefaultBossBar(matrices, width, height, bossBar);
                height += 10 + this.client.textRenderer.fontHeight;
            }
        }
    }

    private void renderCustomBossBar(MatrixStack matrices, int width, int height, CustomBossBar bossBar, BossBar source) {
        float right = (float) width / 2 - (float) bossBar.width() / 2;
        int barHalf = (bossBar.right() - bossBar.left()) / 2;
        UUID uuid = source.getUuid();

        if (activeBossBars.get(uuid) == null || alwaysUpdate.get(uuid) || activeBossBars.get(uuid).progress() != source.getPercent()) {
            updateActiveTextures(bossBar, uuid, source.getPercent());
        }

        Identifier texture = activeBossBars.get(uuid).texture();
        Identifier overlay = activeBossBars.get(uuid).overlay();
        int textureV = activeBossBars.get(uuid).textureOffset();
        int overlayV = activeBossBars.get(uuid).overlayOffset();
        int textureHeight = activeBossBars.get(uuid).textureHeight();
        int overlayHeight = activeBossBars.get(uuid).overlayHeight();

        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, texture);
        DrawableHelper.drawTexture(matrices, (int) right, height, 100,
                0, textureV, bossBar.width(), bossBar.height(), bossBar.width(), textureHeight);

        int barLength = (int) (source.getPercent() * (float) (bossBar.right() - bossBar.left()));

        if (barLength > 0) {
            RenderSystem.setShaderTexture(0, overlay);
            switch (bossBar.type()) {
                case NORMAL -> DrawableHelper.drawTexture(matrices, (int) right + bossBar.left(), height, 110,
                        bossBar.left(), overlayV, barLength, bossBar.height(), bossBar.width(), overlayHeight);

                case REVERSE -> DrawableHelper.drawTexture(matrices, (int) right + bossBar.right() - barLength, height, 110,
                        bossBar.right() - barLength, overlayV, barLength, bossBar.height(), bossBar.width(), overlayHeight);

                case DOUBLE -> DrawableHelper.drawTexture(matrices, (int) right + bossBar.left() + (bossBar.right() - bossBar.left() - barLength) / 2, height, 110,
                        bossBar.left() + (float) (bossBar.right() - bossBar.left() - barLength) / 2, overlayV,
                        barLength, bossBar.height(), bossBar.width(), overlayHeight);

                case DOUBLE_REVERSE -> {
                    DrawableHelper.drawTexture(matrices, (int) right + bossBar.left(), height, 110,
                            bossBar.left(), overlayV, barLength / 2 + 1, bossBar.height(), bossBar.width(), overlayHeight);

                    DrawableHelper.drawTexture(matrices, (int) (right + bossBar.left() + barHalf + (float) (bossBar.right() - bossBar.left() - barLength) / 2) + 1, height, 110,
                            bossBar.left() + barHalf + (float) (bossBar.right() - bossBar.left() - barLength) / 2 + 1, overlayV,
                            barLength / 2, bossBar.height(), bossBar.width(), overlayHeight);
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private void updateActiveTextures(CustomBossBar source, UUID uuid, float progress) {
        ActiveBossBar bossBar = activeBossBars.containsKey(uuid) ? activeBossBars.get(uuid) : new ActiveBossBar();

        float phase = getTextureKey(source.phases(), progress);
        bossBar.setProgress(progress);

        bossBar.setTexture(source.textures().get(phase));
        bossBar.setOverlay(source.overlays().get(phase));

        if (bossBar.phase() == 0.0f || bossBar.phase() != phase || alwaysUpdate.get(uuid))
            activeBossBars.put(uuid, updateTextureParams(source, bossBar, phase));
        else activeBossBars.put(uuid, bossBar);

        if (alwaysUpdate.containsKey(uuid)) return;

        alwaysUpdate.put(uuid, !source.textureFrames().isEmpty() && !source.overlayFrames().isEmpty());
    }

    private ActiveBossBar updateTextureParams(CustomBossBar source, ActiveBossBar bossBar, float phase) {
        bossBar.setPhase(phase);

        bossBar.setTextureOffset(getFrameOffset(renderTime, source.textureFrames(), source.height(), phase));
        bossBar.setOverlayOffset(getFrameOffset(renderTime, source.overlayFrames(), source.height(), phase));

        bossBar.setTextureHeight(source.textureFrames().get(phase) == null ? source.height() :
                source.height() * source.textureFrames().get(phase).length);
        bossBar.setOverlayHeight(source.overlayFrames().get(phase) == null ? source.height() :
                source.height() * source.overlayFrames().get(phase).length);
        return bossBar;
    }

    private void clearActiveTextures() {
        activeBossBars.clear();
        alwaysUpdate.clear();
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

    private void renderDefaultBossBar(MatrixStack matrices, int width, int height, BossBar bossBar) {
        this.renderDefaultBossBar(matrices, width / 2 - 91, height, bossBar, 182, 0);
        int progress = (int) (bossBar.getPercent() * 183.0f);
        if (progress > 0) {
            this.renderDefaultBossBar(matrices, width / 2 - 91, height, bossBar, progress, 5);
        }
        Text text = bossBar.getName();
        int textWidth = this.client.textRenderer.getWidth(text);
        MinecraftClient.getInstance().textRenderer.drawWithShadow(matrices, text, (float) (width / 2 - textWidth / 2), height - 9, 0xFFFFFF);
    }

    private void renderDefaultBossBar(MatrixStack matrices, int x, int y, BossBar bossBar, int width, int height) {
        RenderSystem.setShaderTexture(0, DEFAULT);
        DrawableHelper.drawTexture(matrices, x, y, 0, bossBar.getColor().ordinal() * 5 * 2 + height, width, 5);
        if (bossBar.getStyle() != BossBar.Style.PROGRESS) {
            RenderSystem.enableBlend();
            DrawableHelper.drawTexture(matrices, x, y, 0, 80 + (bossBar.getStyle().ordinal() - 1) * 5 * 2 + height, width, 5);
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
