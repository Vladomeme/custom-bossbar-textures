package net.cbt.main.events;

import net.cbt.main.bossbar.BossBarManager;
import net.cbt.main.bossbar.CustomBossBar;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class EventManager {

    BossBarManager manager;

    public List<BossBarEvent> chatEvents = new ArrayList<>();
    public List<BossBarEvent> addEvents = new ArrayList<>();
    public List<BossBarEvent> removeEvents = new ArrayList<>();

    public EventManager(BossBarManager manager) {
        this.manager = manager;
    }

    public void tick(CustomBossBar source, int height) {
        for (BossBarEvent event : source.events()) {
            event.tick(manager, source, height);
        }
    }

    public void chatCondition(Text text) {
        String message = text.getString();
        for (BossBarEvent event : chatEvents) {
            if (event.condition.equals(message)) {
                event.trigger();
            }
        }
    }

    public void phaseCondition(CustomBossBar source, float prevProgress, float progress) {
        for (BossBarEvent event : source.events()) {
            float condition = (float) event.condition;
            if (progress <= condition && condition < prevProgress) {
                event.trigger();
            }
        }
    }

    public void addCondition(Text text) {
        String name = text.getString();
        for (BossBarEvent event : addEvents) {
            if (event.condition.equals(name)) {
                event.trigger();
            }
        }
    }

    public void removeCondition(Text text) {
        String name = text.getString();
        for (BossBarEvent event : removeEvents) {
            if (event.condition.equals(name)) {
                event.trigger();
            }
        }
    }

    public void clear() {
        chatEvents.clear();
        addEvents.clear();
        removeEvents.clear();
    }
}
