package net.cbt.main.bossbar;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.systems.RenderSystem;
import net.cbt.main.events.EventManager;
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
    private EventManager eventManager;
    public final HashMap<String, CustomBossBar> customBossBars = new HashMap<>();
    public Map<UUID, ClientBossBar> bossBars = Maps.newLinkedHashMap();
    List<String> regexKeys = new ArrayList<>();

    public int renderTime = 0;
    public DrawContext context;

    HashMap<UUID, ActiveBossBar> activeBossBars = Maps.newLinkedHashMap();
    public HashMap<String, Integer> bossBarHeights = Maps.newLinkedHashMap();
    HashMap<UUID, Boolean> alwaysUpdate = Maps.newLinkedHashMap();

    private static final Identifier DEFAULT = new Identifier("textures/gui/bars.png");

    public BossBarManager(MinecraftClient client) {
        this.client = client;
    }

    public void render(DrawContext context) {
        if (this.bossBars.isEmpty()) {
            clearActiveTextures();
            return;
        }
        bossBarHeights.clear();
        int width = context.getScaledWindowWidth();
        int height = 12;
        loop:
        for (BossBar bossBar : this.bossBars.values()) {
            String name = bossBar.getName().getString();
            if (customBossBars.containsKey(name)) {
                if (customBossBars.get(name).type() == Type.HIDDEN) continue;
                this.renderCustomBossBar(context, width, height, customBossBars.get(name), bossBar);
                bossBarHeights.put(name, height);
                height += 5 + customBossBars.get(name).height();
                continue;
            }
            for (String key : regexKeys) {
                if (name.matches(key)) {
                    if (customBossBars.get(key).type() == Type.HIDDEN) continue loop;
                    this.renderCustomBossBar(context, width, height, customBossBars.get(key), bossBar);
                    bossBarHeights.put(name, height);
                    height += 5 + customBossBars.get(key).height();
                    continue loop;
                }
            }
            this.renderDefaultBossBar(context, width, height, bossBar);
            bossBarHeights.put(name, height);
            height += 10 + this.client.textRenderer.fontHeight;
        }
        eventManager.tick();
        this.context = context;
    }

    private void renderCustomBossBar(DrawContext context, int width, int height, CustomBossBar bossBar, BossBar source) {
        float right = (float) width / 2 - (float) bossBar.width() / 2;
        int barHalf = (bossBar.right() - bossBar.left()) / 2;
        UUID uuid = source.getUuid();

        if (activeBossBars.get(uuid).progress() != source.getPercent()) {
            eventManager.phaseCondition(uuid, activeBossBars.get(uuid).progress(), source.getPercent());
        }

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
        regexKeys.clear();
    }

    public void setCustomBossBars(ArrayList<CustomBossBar> source) {
        for (CustomBossBar bossBar : source) {
            if (bossBar.name().startsWith("regex")) {
                String name = bossBar.name().replace("regex:", "");
                this.customBossBars.put(name, bossBar.withName(name));
                this.regexKeys.add(name);
                continue;
            }
            this.customBossBars.put(bossBar.name(), bossBar);
        }
    }

    public void setBossBars(BossBarHud source) {
        this.bossBars = source.bossBars;

    }

    public void setEventManager(EventManager manager) {
        this.eventManager = manager;

    }

    public void tick() {
        this.renderTime++;
    }

    public enum Type {
        NORMAL,
        REVERSE,
        DOUBLE,
        DOUBLE_REVERSE,
        HIDDEN
    }
}
