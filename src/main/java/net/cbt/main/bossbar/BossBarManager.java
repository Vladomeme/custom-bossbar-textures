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

public class BossBarManager {

    private final MinecraftClient client;
    final HashMap<String, CustomBossBar> customBossBars = new HashMap<>();
    Map<UUID, ClientBossBar> bossBars = Maps.newLinkedHashMap();

    private static final Identifier DEFAULT = new Identifier("textures/gui/bars.png");

    public BossBarManager(MinecraftClient client) {
        this.client = client;
    }

    public void render(DrawContext context) {
        if (this.bossBars.isEmpty()) {
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

        float phase = getTextureKey(bossBar.phases(), source.getPercent());
        Identifier texture = bossBar.textures().get(phase);
        Identifier overlay = bossBar.overlays().get(phase);

        RenderSystem.enableBlend();
        context.drawTexture(texture, (int) right, height, 100,
                0, 0, bossBar.width(), bossBar.height(), bossBar.width(), bossBar.height());

        int barLength = (int) (source.getPercent() * (float) (bossBar.right() - bossBar.left()));

        if (barLength > 0) {
            switch (bossBar.type()) {
                case NORMAL -> context.drawTexture(overlay, (int) right + bossBar.left(), height, 110,
                        bossBar.left(), 0, barLength, bossBar.height(), bossBar.width(), bossBar.height());

                case REVERSE -> context.drawTexture(overlay, (int) right + bossBar.right() - barLength, height, 110,
                        bossBar.right() - barLength, 0, barLength, bossBar.height(), bossBar.width(), bossBar.height());

                case DOUBLE -> context.drawTexture(overlay, (int) right + bossBar.left() + (bossBar.right() - bossBar.left() - barLength) / 2, height, 110,
                        bossBar.left() + (float) (bossBar.right() - bossBar.left() - barLength) / 2, 0,
                        barLength, bossBar.height(), bossBar.width(), bossBar.height());

                case DOUBLE_REVERSE -> {
                    context.drawTexture(overlay, (int) right + bossBar.left(), height, 110,
                            bossBar.left(), 0, barLength / 2 + 1, bossBar.height(), bossBar.width(), bossBar.height());

                    context.drawTexture(overlay, (int) (right + bossBar.left() + barHalf + (float) (bossBar.right() - bossBar.left() - barLength) / 2) + 1, height, 110,
                            bossBar.left() + barHalf + (float) (bossBar.right() - bossBar.left() - barLength) / 2 + 1, 0,
                            barLength / 2, bossBar.height(), bossBar.width(), bossBar.height());
                }
            }
        }
        RenderSystem.disableBlend();
    }

    private float getTextureKey(List<Float> splits, float percent) {
        for (Float split : splits) {
            if (percent * 100 < split) return split;
        }
        return 100.0f;
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
        this.customBossBars.clear();
    }

    public void setCustomBossBars(ArrayList<CustomBossBar> source) {
        for (CustomBossBar bossBar : source) {
            this.customBossBars.put(bossBar.name(), bossBar);
        }
    }

    public void setBossBars(BossBarHud source) {
        this.bossBars = source.bossBars;
    }

    public enum Type {
        NORMAL,
        REVERSE,
        DOUBLE,
        DOUBLE_REVERSE
    }
}
