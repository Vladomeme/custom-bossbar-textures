package net.cbt.main.bossbar;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;

//Looking at this thing makes me think I'm doing something wrong, and I most likely do.
public record CustomBossBar(BossBarManager.Type type, String name, List<Float> phases,
                            HashMap<Float, Identifier> textures, HashMap<Float, int[]> textureFrames,
                            HashMap<Float, Identifier> overlays, HashMap<Float, int[]> overlayFrames,
                            int width, int height, int left, int right) {

    public CustomBossBar withName(String name) {
        return new CustomBossBar(type, name, phases, textures, textureFrames, overlays, overlayFrames, width, height, left, right);
    }
}
