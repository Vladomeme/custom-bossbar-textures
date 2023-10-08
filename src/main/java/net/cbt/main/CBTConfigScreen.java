package net.cbt.main;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class CBTConfigScreen {
        public static Screen create(Screen parent) {

                CBTConfig currentConfig = CBTConfig.INSTANCE, defaultConfig = new CBTConfig();

                ConfigBuilder builder = ConfigBuilder.create()
                        .setParentScreen(parent)
                        .setTitle(Text.of("Custom Bossbar Textures"))
                        .setSavingRunnable(currentConfig::write);

                ConfigCategory category = builder.getOrCreateCategory(Text.of(""));
                ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                category.addEntry(entryBuilder.startBooleanToggle(Text.of("Enabled"), currentConfig.enabled)
                        .setSaveConsumer(newConfig -> currentConfig.enabled = newConfig)
                        .setDefaultValue(defaultConfig.enabled)
                        .build());

                category.addEntry(entryBuilder.startBooleanToggle(Text.of("Debug"), currentConfig.debug)
                        .setTooltip(Text.of("Enables some debug messages in game log."))
                        .setSaveConsumer(newConfig -> currentConfig.debug = newConfig)
                        .setDefaultValue(defaultConfig.debug)
                        .build());

                return builder.build();
        }
}
