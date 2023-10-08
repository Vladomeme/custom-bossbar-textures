package net.cbt.main.bossbar;

import net.minecraft.util.Identifier;

public record CustomBossBar(BossBarManager.Type type, String name, Identifier texture, Identifier overlay,
                            int width, int height, int left, int right) {

}
