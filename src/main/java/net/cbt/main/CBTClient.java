package net.cbt.main;

import com.google.common.collect.Maps;
import net.cbt.main.bossbar.CustomBossBar;
import net.cbt.main.bossbar.BossBarManager;
import net.cbt.main.events.BossBarEvent;
import net.cbt.main.events.EventManager;
import net.cbt.main.mixin.CBTTransmitter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class CBTClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("cbt");

    public BossBarManager bossbarManager;
    public EventManager eventManager;

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> this.bossbarManager.tick());

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {

            @Override
            public Identifier getFabricId() {
                return new Identifier("cbt", "resources");
            }

            @Override
            public void reload(ResourceManager manager) {
                findBossbars(manager);
            }
        });

        ((CBTTransmitter) MinecraftClient.getInstance().inGameHud).cbt$setClient(this);
        ((CBTTransmitter) MinecraftClient.getInstance().inGameHud.getChatHud()).cbt$setClient(this);

        LOGGER.info("Loading Custom Bossbar Textures, truly monumentous.");
    }

    public void findBossbars(ResourceManager resourceManager) {

        bossbarManager.clear();
        eventManager.clear();

        ArrayList<CustomBossBar> bossbars = new ArrayList<>();

        resourceManager.findResources("cbt", id -> id.getPath().endsWith(".bossbar")).keySet().forEach(id -> {
            CustomBossBar bossBar = readBossbar(resourceManager, id);
            if (bossBar != null) bossbars.add(bossBar);
        });

        bossbarManager.setCustomBossBars(bossbars);
    }

    public CustomBossBar readBossbar(ResourceManager resourceManager, Identifier id) {

        HashMap<String, String> properties = new HashMap<>();
        HashMap<Float, int[]> textureFrames = new HashMap<>();
        HashMap<Float, int[]> overlayFrames = new HashMap<>();
        HashMap<Integer, BossBarEvent> events = new HashMap<>();
        List<Float> splits = new ArrayList<>();

        try {
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.getResource(id).get().getInputStream()));

            while ((line = reader.readLine()) != null) {

                if (line.equals("") || line.startsWith("#")) continue;

                if (line.startsWith("event")) {
                    events = readEventLine(events, line, id);
                    if (events != null) return null;
                }

                String[] entry = line.split("=", 2);
                if (entry[0].length() == 4) {
                    properties.put(entry[0], entry[1]);
                    continue;
                }

                if (entry[0].length() == 7) {
                    splits.add(100.0f);
                    properties.put(entry[0] + ".100.0",
                            id.getPath().substring(0, id.getPath().lastIndexOf("/") + 1) + entry[1] + ".png");
                    continue;
                }

                String[] entrySub = entry[0].split("\\.", 2);
                if (entry[0].endsWith("s")) {
                    Float key = entrySub[1].length() == 6 ? 100.0f :
                            Float.parseFloat(entrySub[1].replace(",", ".").replace(".frames", ""));
                    int[] frames = Arrays.stream(entry[1].replace(" ", "").split(",")).mapToInt(Integer::parseInt).toArray();

                    if (entrySub[0].equals("texture")) {
                        textureFrames.put(key, frames);
                        continue;
                    }
                    overlayFrames.put(key, frames);
                    continue;
                }

                splits.add(Float.parseFloat(entrySub[1].replace(",", ".")));
                properties.put(entry[0].replace(",", "."),
                        id.getPath().substring(0, id.getPath().lastIndexOf("/") + 1) + entry[1] + ".png");
            }
            Collections.sort(splits);
        } catch (IOException e) {
            //don't care
        }
        return buildBossbar(properties, splits.stream().distinct().collect(Collectors.toList()),
                events.values().stream().toList(), textureFrames, overlayFrames, id);
    }

    public HashMap<Integer, BossBarEvent> readEventLine(HashMap<Integer, BossBarEvent> events, String line, Identifier id) {
        String[] entry = line.split("=", 2);
        String[] description = entry[0].split("\\.", 0);
        String[] condition = entry[1].split(":", 2);

        int number;
        try {
            number = Integer.parseInt(description[1]);
        } catch (Exception e) {
            LOGGER.error("Bossbar properties file " + id + "does not specify an event number in line " + line + ".");
            return null;
        }

        BossBarEvent event = events.containsKey(number) ? events.get(number) :
                new BossBarEvent(null, null, new int[]{}, null, null, false);

        switch (description[2]) {
            case "texture" -> {
                if (description.length == 3) {
                    event.setTexture(new Identifier("minecraft",
                            id.getPath().substring(0, id.getPath().lastIndexOf("/") + 1) + entry[1] + ".png"));
                } else {
                    event.setFrames(Arrays.stream(entry[1].replace(" ", "")
                            .split(",")).mapToInt(Integer::parseInt).toArray());
                }
            }
            case "enable", "disable", "play" -> {
                event.setAction(BossBarEvent.Action.valueOf(description[2].toUpperCase()));
                try {
                    event.setType(BossBarEvent.Type.valueOf(condition[0].toUpperCase()));
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Unknown event type " + condition[0] + " in properties file " + id);
                    return null;
                }
                if (condition[0].equals("phase")) event.setCondition(Float.parseFloat(condition[1]));
                else event.setCondition(condition[1]);
            }
            case "default" -> {
                if (!entry[1].equals("active"))
                    LOGGER.error("Property \"default\" could only have value \"active\" in properties file " + id + ". Using it instead.");
                event.setDefault(true);
            }
            default -> {
                LOGGER.error("Unknown property " + entry[2] + " in bossbar properties file " + id);
                return null;
            }
        }
        events.put(number, event);

        return events;
    }
    public CustomBossBar buildBossbar(HashMap<String, String> properties, List<Float> splits, List<BossBarEvent> events,
                                             HashMap<Float, int[]> textureFrames, HashMap<Float, int[]> overlayFrames, Identifier id) {
        int width;
        int height;
        int left = 9999;
        int right = 0;

        HashMap<Float, Identifier> textures = Maps.newLinkedHashMap();
        HashMap<Float, Identifier> overlays = Maps.newLinkedHashMap();

        BossBarManager.Type type = BossBarManager.Type.NORMAL;
        try {
            type = BossBarManager.Type.valueOf(properties.get("type").toUpperCase().replace(" ", ""));
        } catch (IllegalArgumentException e) {
            CBTClient.LOGGER.error("Bossbar properties file " + id + "does not contain a valid bossbar type. " +
                    "Using NORMAL type.");
        }

        if (!properties.containsKey("name")) {
            CBTClient.LOGGER.error("Bossbar properties file " + id + "should specify bossbar display name.");
            return null;
        }
        if (type == BossBarManager.Type.HIDDEN) {
            if (CBTConfig.INSTANCE.debug) {
                sendBuildMessage(properties, id, events);
            }

            return new CustomBossBar(
                    type, properties.get("name"), null, events, null, null,
                    null, null, 0, 0, 0, 0);
        }

        if (splits.isEmpty()) {
            LOGGER.error("Texture path doesn't exist or invalid in file: " + id);
            return null;
        }
        else {
            float fallback;
            for (Float percent : splits) {
                try {
                    if (MinecraftClient.getInstance().getResourceManager().getResource(
                            new Identifier("minecraft", properties.get("texture." + percent))).isEmpty()) {
                        LOGGER.error("Texture path for " + percent + "% HP is invalid in file: " + id);
                        return null;
                    }
                    textures.put(percent, new Identifier("minecraft", properties.get("texture." + percent)));
                } catch (CompletionException | NullPointerException e) {
                    LOGGER.warn("Bossbar file has no texture path for " + percent + "% HP: " + id);
                    fallback = getFallbackSplit(splits, percent);
                    if (fallback == -1.0f) {
                        LOGGER.error("Unable to find a fallback texture. Skipping bossbar " + id);
                        return null;
                    }
                    LOGGER.warn("Using a fallback texture (" + fallback + ")");
                    textures.put(percent, new Identifier("minecraft", properties.get("texture." + fallback)));
                }
                try {
                    if (MinecraftClient.getInstance().getResourceManager().getResource(
                            new Identifier("minecraft", properties.get("overlay." + percent))).isEmpty()) {
                        LOGGER.error("Overlay path for " + percent + "% HP is invalid in file: " + id);
                        return null;
                    }
                    overlays.put(percent, new Identifier("minecraft", properties.get("overlay." + percent)));
                } catch (CompletionException | NullPointerException e) {
                    LOGGER.warn("Bossbar file has no overlay path for " + percent + "% HP: " + id);
                    fallback = getFallbackSplit(splits, percent);
                    if (fallback == -1.0f) {
                        LOGGER.error("Unable to find a fallback overlay. Skipping bossbar " + id);
                        return null;
                    }
                    LOGGER.warn("Using a fallback overlay (" + fallback + ")");
                    overlays.put(percent, new Identifier("minecraft", properties.get("overlay." + fallback)));
                }
            }
        }

        try {
            BufferedImage overlay = ImageIO.read(MinecraftClient.getInstance().getResourceManager().getResource(
                    new Identifier("minecraft", properties.get("overlay." + splits.get(0)))).get().getInputStream());
            width = overlay.getWidth();
            height = overlayFrames.containsKey(splits.get(0)) ?
                    overlay.getHeight() / overlayFrames.get(splits.get(0)).length : overlay.getHeight();
            for (int i = 0; i < height; i++) {
                for (int k = 0; k < width; k++) {
                    if ((overlay.getRGB(k, i) >> 24) == 0x00 || k >= left) {
                        continue;
                    }
                    left = k;
                }
                for (int k = width - 1; k > 0; k--) {
                    if ((overlay.getRGB(k, i) >> 24) == 0x00 || k <= right) {
                        continue;
                    }
                    right = k;
                }
            }
        } catch (IOException | NoSuchElementException | ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Overlay path doesn't exist or invalid in file: " + id);
            return null;
        }

        if (CBTConfig.INSTANCE.debug) {
            sendBuildMessage(splits, properties, id, events, textureFrames, overlayFrames, width, height, left, right);
        }

        registerEvents(events);
        
        return new CustomBossBar(
                type, properties.get("name"), splits, events,
                textures, textureFrames,
                overlays, overlayFrames,
                width, height, left, right);
    }

    private void registerEvents(List<BossBarEvent> events) {
        for (BossBarEvent event : events) {
            switch (event.type) {
                case CHAT -> eventManager.chatEvents.add(event);
                case ADD -> eventManager.addEvents.add(event);
                case REMOVE -> eventManager.removeEvents.add(event);
            }
        }
    }

    private float getFallbackSplit(List<Float> splits, Float percent) {
        for (Float option : splits) {
            if (percent < option) return option;
        }
        return -1.0f;
    }

    public void sendBuildMessage(List<Float> splits, HashMap<String, String> properties, Identifier id, List<BossBarEvent> events,
                                        HashMap<Float, int[]> textureFrames, HashMap<Float, int[]> overlayFrames,
                                        int width, int height, int left, int right) {
        StringBuilder texturesLine = new StringBuilder();
        StringBuilder overlaysLine = new StringBuilder();
        StringBuilder textureFramesLine = new StringBuilder();
        StringBuilder overlayFramesLine = new StringBuilder();
        StringBuilder eventsLine = new StringBuilder();

        if (splits.isEmpty()) {
            texturesLine.append("\nTexture: ").append(new Identifier("minecraft", properties.get("texture")));
            overlaysLine.append("\nOverlay: ").append(new Identifier("minecraft", properties.get("overlay")));
        }
        else {
            texturesLine.append("\nTextures: ");
            overlaysLine.append("\nOverlays: ");
            for (Float split : splits) {
                try {
                    texturesLine.append(split).append(": ")
                            .append(new Identifier("minecraft", properties.get("texture." + split))).append(", ");
                    overlaysLine.append(split).append(": ")
                            .append(new Identifier("minecraft", properties.get("overlay." + split))).append(", ");
                } catch (CompletionException | NullPointerException e) {
                    //don't care
                }
            }
        }

        if (!textureFrames.isEmpty()) {
            textureFramesLine.append("\nTexture frames: ");
            textureFrames.forEach((key, frames) -> textureFramesLine.append(key).append(":").append(Arrays.toString(frames)).append(" "));
        }
        if (!overlayFrames.isEmpty()) {
            overlayFramesLine.append("\nOverlay frames: ");
            overlayFrames.forEach((key, frames) -> overlayFramesLine.append(key).append(":").append(Arrays.toString(frames)).append(" "));
        }

        if (!events.isEmpty()) {
            eventsLine.append("\nEvents:");
            for (BossBarEvent event : events) {
                eventsLine.append("Type: ").append(event.type).append(", action: ").append(event.action)
                        .append(", condition: ").append(event.condition).append(", texture:").append(event.texture)
                        .append(", frames: ").append(Arrays.toString(event.frames)).append("\n");
            }
        }

        LOGGER.info("Building bossbar: " + id.toString() +
                "\nType: " + BossBarManager.Type.valueOf(properties.get("type").toUpperCase()) +
                "\nDisplay name: " + properties.get("name") +
                texturesLine +
                overlaysLine +
                textureFramesLine +
                overlayFramesLine +
                "\nTexture width = " + width + ", height = " + height +
                "\nOverlay borders: left = " + left + ", right = " + right +
                eventsLine);
    }

    public void sendBuildMessage(HashMap<String, String> properties, Identifier id, List<BossBarEvent> events) {
        StringBuilder eventsLine = new StringBuilder();

        if (!events.isEmpty()) {
            eventsLine.append("\nEvents:");
            for (BossBarEvent event : events) {
                eventsLine.append("Type: ").append(event.type).append(", action: ").append(event.action)
                        .append(", condition: ").append(event.condition).append(", texture:").append(event.texture)
                        .append(", frames: ").append(Arrays.toString(event.frames)).append("\n");
            }
        }

        LOGGER.info("Building bossbar: " + id.toString() +
                "\nType: " + BossBarManager.Type.valueOf(properties.get("type").toUpperCase()) +
                "\nDisplay name: " + properties.get("name") +
                eventsLine);
    }
}
