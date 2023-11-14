package net.cbt.main;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;

public class CBTConfig {

    public boolean enabled = true;
    public boolean debug = true;

    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "cbt.json");

    public static final CBTConfig INSTANCE = read();

    public static CBTConfig read() {
        if (!FILE.exists())
            return new CBTConfig().write();

        Reader reader = null;
        try {
            return new Gson().fromJson(reader = new FileReader(FILE), CBTConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    public CBTConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(FILE));
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(this, CBTConfig.class), writer);
        } catch (Exception e) {
            CBTClient.LOGGER.error("Couldn't save config");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return this;
    }

}
