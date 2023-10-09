package net.cbt.main;

import com.google.common.collect.Maps;
import net.cbt.main.bossbar.CustomBossBar;
import net.cbt.main.bossbar.BossBarManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class CBTClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("cbt");

    public static BossBarManager bossbarManager;

    @Override
    public void onInitializeClient() {

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

        LOGGER.info("Loading Custom Bossbar Textures, truly monumentous.");
    }

    public static void findBossbars(ResourceManager resourceManager) {

        bossbarManager.clear();

        HashMap<String, String> properties = new HashMap<>();
        List<Float> splits = new ArrayList<>();
        ArrayList<CustomBossBar> bossbars = new ArrayList<>();

        resourceManager.findResources("cbt", id -> id.getPath().endsWith(".bossbar")).keySet().forEach(id -> {
            try {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.getResource(id).get().getInputStream()));

                while ((line = reader.readLine()) != null) {
                    if (line.equals("") || line.startsWith("#")) continue;
                    String[] entry = line.split("=", 2);
                    if (entry[0].startsWith("te") || entry[0].startsWith("ov")) {
                        if (entry[0].length() > 7 && entry[0].charAt(7) == '.') {
                            splits.add(Float.valueOf(entry[0].split("\\.", 2)[1].replace(",", ".")));
                            properties.put(entry[0].replace(",", "."), id.getPath().substring(0, id.getPath().lastIndexOf("/") + 1) + entry[1] + ".png");
                        }
                        else {
                            splits.add(100.0f);
                            properties.put(entry[0] + ".100.0", id.getPath().substring(0, id.getPath().lastIndexOf("/") + 1) + entry[1] + ".png");
                        }
                    }
                    else {
                        properties.put(entry[0], entry[1]);
                    }
                }
                Collections.sort(splits);
                CustomBossBar bossBar = buildBossbar(properties, splits.stream().distinct().collect(Collectors.toList()), id);
                if (bossBar != null) {
                    bossbars.add(bossBar);
                }
                properties.clear();
                splits.clear();
            } catch (IOException e) {
               //don't care
            }
        });

        bossbarManager.setCustomBossBars(bossbars);
    }

    public static CustomBossBar buildBossbar(HashMap<String, String> properties, List<Float> splits, Identifier id) {
        int width;
        int height;
        int left = 9999;
        int right = 0;

        HashMap<Float, Identifier> textures = Maps.newLinkedHashMap();
        HashMap<Float, Identifier> overlays = Maps.newLinkedHashMap();

        try {
            BossBarManager.Type.valueOf(properties.get("type").toUpperCase());
        } catch (IllegalArgumentException e) {
            CBTClient.LOGGER.error("Bossbar properties file " + id + "should specify a valid bossbar type.");
            return null;
        }

        if (!properties.containsKey("name")) {
            CBTClient.LOGGER.error("Bossbar properties file " + id + "should specify bossbar display name.");
            return null;
        }

        if (splits.isEmpty()) {
            LOGGER.error("Texture path doesn't exist or invalid in file: " + id);
            return null;
        }
        else {
            for (Float percent : splits) {
                if (MinecraftClient.getInstance().getResourceManager().getResource(
                        new Identifier("minecraft", properties.get("texture." + percent))).isEmpty()) {
                    LOGGER.error("Texture path for " + percent + "% HP is invalid in file: " + id);
                    return null;
                }
                else textures.put(percent, new Identifier("minecraft", properties.get("texture." + percent)));
                if (MinecraftClient.getInstance().getResourceManager().getResource(
                        new Identifier("minecraft", properties.get("overlay." + percent))).isEmpty()) {
                    LOGGER.error("Overlay path for " + percent + "% HP is invalid in file: " + id);
                    return null;
                }
                else overlays.put(percent, new Identifier("minecraft", properties.get("overlay." + percent)));
            }
        }

        try {
            BufferedImage overlay = ImageIO.read(MinecraftClient.getInstance().getResourceManager().getResource(
                    new Identifier("minecraft", properties.get("overlay."+ splits.get(0)))).get().getInputStream());
            width = overlay.getWidth();
            height = overlay.getHeight();
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
            sendBuildMessage(splits, properties, id, width, height, left, right);
        }

        return new CustomBossBar(
                BossBarManager.Type.valueOf(properties.get("type").toUpperCase()),
                properties.get("name"),
                splits, textures, overlays,
                width, height, left, right);
    }

    public static void sendBuildMessage(List<Float> splits, HashMap<String, String> properties, Identifier id,
                                        int width, int height, int left, int right) {
        StringBuilder splitsLine = new StringBuilder();
        StringBuilder texturesLine = new StringBuilder();
        StringBuilder overlaysLine = new StringBuilder();

        if (splits.isEmpty()) {
            texturesLine.append("\nTexture: ").append(new Identifier("minecraft", properties.get("texture")));
            overlaysLine.append("\nOverlay: ").append(new Identifier("minecraft", properties.get("overlay")));
        }
        else {
            splitsLine.append("\nSplits: ");
            texturesLine.append("\nTextures: ");
            overlaysLine.append("\nOverlays: ");
            for (Float split : splits) {
                splitsLine.append(split).append(", ");
                texturesLine.append(new Identifier("minecraft", properties.get("texture." + split))).append(", ");
                overlaysLine.append(new Identifier("minecraft", properties.get("overlay." + split))).append(", ");
            }
        }

        LOGGER.info("Building bossbar: " + id.toString() +
                "\nType: " + BossBarManager.Type.valueOf(properties.get("type").toUpperCase()) +
                "\nDisplay name: " + properties.get("name") +
                splitsLine +
                texturesLine +
                overlaysLine +
                "\nTexture width = " + width + ", height = " + height +
                "\nOverlay borders: left = " + left + ", right = " + right);
    }
}
