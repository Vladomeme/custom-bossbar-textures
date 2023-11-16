package net.cbt.main.events;

import com.mojang.blaze3d.systems.RenderSystem;
import net.cbt.main.bossbar.BossBarManager;
import net.cbt.main.bossbar.CustomBossBar;
import net.minecraft.util.Identifier;

import java.util.stream.IntStream;

public class BossBarEvent {

    public Object condition;
    public Identifier texture;
    public int[] frames;
    public Type type;
    public Action action;
    public boolean active;

    int duration;
    int localRenderTime = 0;

    public BossBarEvent(Object condition, Identifier texture,
                        int[] frames, Type type, Action action, boolean active) {
        this.condition = condition;
        this.texture = texture;
        this.frames = frames;
        this.type = type;
        this.action = action;
        this.active = active;

        this.duration = IntStream.of(frames).sum();
    }

    public void trigger() {
        switch (action) {
            case ENABLE, PLAY -> active = true;
            case DISABLE -> active = false;
        }
    }

    public void tick(BossBarManager manager, CustomBossBar source, int height) {
        if (!active) return;
        if (action == Action.PLAY) {
            if (localRenderTime == duration) {
                this.active = false;
                localRenderTime = 0;
                return;
            }
            localRenderTime++;
        }
        this.render(manager, source, height);
    }

    private void render(BossBarManager manager, CustomBossBar bossBar, int height) {
        int width = manager.context.getScaledWindowWidth();
        int right = width / 2 - bossBar.width() / 2;
        int textureV = getFrameOffset(action == Action.PLAY ? localRenderTime : manager.renderTime, bossBar.height());
        int textureHeight = frames == null ? bossBar.height() :
                bossBar.height() * frames.length;

        RenderSystem.enableBlend();
        manager.context.drawTexture(texture, right, height, 100,
                0, textureV, bossBar.width(), bossBar.height(), bossBar.width(), textureHeight);
        RenderSystem.disableBlend();
    }

    private int getFrameOffset(long renderTime, int height) {
        if (frames == null || frames.length == 0) return 0;

        int progress = (int) renderTime % duration;
        int counter = 0;

        for (int i = 0; i < frames.length; i++) {
            counter += frames[i];
            if (counter > progress) return i * height;
        }
        return 0;
    }

    public enum Type {
        PHASE,
        CHAT,
        ADD,
        REMOVE
    }

    public enum Action {
        ENABLE,
        DISABLE,
        PLAY
    }

    public void setCondition(Object condition) {
        this.condition = condition;
    }

    public void setTexture(Identifier texture) {
        this.texture = texture;
    }

    public void setFrames(int[] frames) {
        this.frames = frames;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setDefault(boolean active) {
        this.active = active;
    }
}
