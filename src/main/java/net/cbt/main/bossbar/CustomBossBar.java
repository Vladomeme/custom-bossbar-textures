package net.cbt.main.bossbar;

import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;

public record CustomBossBar(BossBarManager.Type type, String name,
                            List<Float> phases, HashMap<Float, Identifier> textures, HashMap<Float, Identifier> overlays,
                            int width, int height, int left, int right) {

}
