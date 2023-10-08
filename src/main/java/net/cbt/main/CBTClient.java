package net.cbt.main;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

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
        ArrayList<CustomBossBar> bossbars = new ArrayList<>();

        resourceManager.findResources("cbt", id -> id.getPath().endsWith(".bossbar")).keySet().forEach(id -> {
            try {
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceManager.getResource(id).get().getInputStream()));

                while ((line = reader.readLine()) != null) {
                    String[] entry = line.split("=", 2);
                    if (entry[0].equals("texture") || entry[0].equals("overlay")) {
                        properties.put(entry[0], id.getPath().substring(0, id.getPath().lastIndexOf("/") + 1) + entry[1] + ".png");
                    }
                    else {
                        properties.put(entry[0], entry[1]);
                    }
                }
                CustomBossBar bossBar = buildBossbar(properties, id);
                if (bossBar != null) {
                    bossbars.add(bossBar);
                }
            } catch (IOException e) {
               //don't care
            }
        });

        bossbarManager.setCustomBossBars(bossbars);
    }

    public static CustomBossBar buildBossbar(HashMap<String, String> properties, Identifier id) {
        int width;
        int height;
        int left = 9999;
        int right = 0;

        if (!properties.containsKey("type")) {
            CBTClient.LOGGER.error("Bossbar properties file " + id + "should specify bossbar type.");
            return null;
        }
        if (!properties.containsKey("name")) {
            CBTClient.LOGGER.error("Bossbar properties file " + id + "should specify bossbar display name.");
            return null;
        }

         if (MinecraftClient.getInstance().getResourceManager().getResource(
                 new Identifier("minecraft", properties.get("texture"))).isEmpty()) {
            LOGGER.error("Texture path doesn't exist or invalid in file: " + id);
            return null;
        }

        try {
            BufferedImage overlay = ImageIO.read(MinecraftClient.getInstance().getResourceManager().getResource(
                    new Identifier("minecraft", properties.get("overlay"))).get().getInputStream());
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
            CBTClient.LOGGER.error("Overlay path doesn't exist or invalid in file: " + id);
            return null;
        }

        if (CBTConfig.INSTANCE.debug) {
            LOGGER.info("Building bossbar: " + id.toString() +
                    "\nType: " + BossBarManager.Type.valueOf(properties.get("type").toUpperCase()) +
                    "\nDisplay name: " + properties.get("name") +
                    "\nTexture: " + new Identifier("minecraft", properties.get("texture")) +
                    "\nOverlay: " + new Identifier("minecraft", properties.get("overlay")) +
                    "\nTexture width = " + width + ", height = " + height +
                    "\nOverlay borders: left = " + left + ", right = " + right);
        }

        return new CustomBossBar(
                BossBarManager.Type.valueOf(properties.get("type").toUpperCase()),
                properties.get("name"),
                new Identifier("minecraft", properties.get("texture")),
                new Identifier("minecraft", properties.get("overlay")),
                width, height, left, right);
    }
}
