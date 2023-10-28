package net.cbt.main.bossbar;

import net.minecraft.util.Identifier;

public class ActiveBossBar {
    Identifier texture;
    Identifier overlay;
    int textureOffset;
    int overlayOffset;
    int textureHeight;
    int overlayHeight;
    float progress;
    float phase;

    public Identifier texture() {
        return texture;
    }

    public Identifier overlay() {
        return overlay;
    }

    public int textureOffset() {
        return textureOffset;
    }

    public int overlayOffset() {
        return overlayOffset;
    }

    public int textureHeight() {
        return textureHeight;
    }

    public int overlayHeight() {
        return overlayHeight;
    }

    public float progress() {
        return progress;
    }

    public float phase() {
        return phase;
    }

    public void setTexture(Identifier texture) {
        this.texture = texture;
    }

    public void setOverlay(Identifier overlay) {
        this.overlay = overlay;
    }

    public void setTextureOffset(int offset) {
        this.textureOffset = offset;
    }

    public void setOverlayOffset(int offset) {
        this.overlayOffset = offset;
    }

    public void setTextureHeight(int height) {
        this.textureHeight = height;
    }

    public void setOverlayHeight(int height) {
        this.overlayHeight = height;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public void setPhase(float phase) {
        this.phase = phase;
    }
}
