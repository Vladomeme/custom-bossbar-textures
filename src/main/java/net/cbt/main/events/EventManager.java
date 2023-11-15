package net.cbt.main.events;

import com.google.common.collect.Maps;
import net.cbt.main.CBTClient;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.UUID;

public class EventManager {

    CBTClient client;

    HashMap<UUID, BossBarEvent> chatEvents = Maps.newLinkedHashMap();
    HashMap<UUID, BossBarEvent> phaseEvents = Maps.newLinkedHashMap();
    HashMap<UUID, BossBarEvent> addEvents = Maps.newLinkedHashMap();
    HashMap<UUID, BossBarEvent> removeEvents = Maps.newLinkedHashMap();

    public EventManager(CBTClient client) {
        this.client = client;
    }

    public void tick() {
        for (BossBarEvent event : chatEvents.values()) {
            event.tick(client);
        }
        for (BossBarEvent event : phaseEvents.values()) {
            event.tick(client);
        }
        for (BossBarEvent event : addEvents.values()) {
            event.tick(client);
        }
        for (BossBarEvent event : removeEvents.values()) {
            event.tick(client);
        }
    }

    public void chatCondition(Text text) {
        String message = text.getString();
        for (BossBarEvent event : chatEvents.values()) {
            if (event.condition.equals(message)) {
                event.trigger();
            }
        }
    }

    public void phaseCondition(UUID uuid, float prevProgress, float progress) {
        float condition = (float) phaseEvents.get(uuid).condition;
        if (progress <= condition && condition < prevProgress) {
            phaseEvents.get(uuid).trigger();
        }
    }

    public void addCondition(Text text) {
        String name = text.getString();
        for (BossBarEvent event : addEvents.values()) {
            if (event.condition.equals(name)) {
                event.trigger();
            }
        }
    }

    public void removeCondition(Text text) {
        String name = text.getString();
        for (BossBarEvent event : removeEvents.values()) {
            if (event.condition.equals(name)) {
                event.trigger();
            }
        }
    }

    public void clear() {
        chatEvents.clear();
        phaseEvents.clear();
        addEvents.clear();
        removeEvents.clear();
    }
}
