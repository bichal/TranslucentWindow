package net.bichal.translucent_window;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@Environment(EnvType.CLIENT)
public class TranslucentWindowConfig {
    public static final int EFFECT_MICA = 1;

    private final Color windowTintColor = new Color(0, 0, 0, 0);

    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "translucent_window.json");

    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(Color.class, new ColorTypeAdapter()).setPrettyPrinting().create();

    private static class ColorTypeAdapter extends TypeAdapter<Color> {
        @Override
        public void write(JsonWriter out, Color color) throws IOException {
            if (color == null) {
                out.nullValue();
                return;
            }
            out.beginObject();
            out.name("r").value(color.getRed());
            out.name("g").value(color.getGreen());
            out.name("b").value(color.getBlue());
            out.name("a").value(color.getAlpha());
            out.endObject();
        }

        @Override
        public Color read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }

            int r = 0, g = 0, b = 0, a = 255;
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "r":
                        r = in.nextInt();
                        break;
                    case "g":
                        g = in.nextInt();
                        break;
                    case "b":
                        b = in.nextInt();
                        break;
                    case "a":
                        a = in.nextInt();
                        break;
                    default:
                        in.skipValue();
                }
            }
            in.endObject();
            return new Color(r, g, b, a);
        }
    }

    private static TranslucentWindowConfig instance;

    public static TranslucentWindowConfig getInstance() {
        if (instance == null) {
            instance = loadConfig();
        }
        return instance;
    }

    public int getEffectType() {
        return EFFECT_MICA;
    }

    public String getWindowTitle() {
        return "Minecraft";
    }

    public Color getWindowTintColor() {
        return windowTintColor;
    }

    public boolean isShowModCount() {
        return false;
    }

    public int getDwmBackdropType() {
        return TranslucentWindowClient.DwmApi.DWMSBT_MAINWINDOW;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            TranslucentWindowClient.LOGGER.error("Failed to save config", e);
        }
    }

    private static TranslucentWindowConfig loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                TranslucentWindowConfig config = GSON.fromJson(reader, TranslucentWindowConfig.class);
                TranslucentWindowClient.LOGGER.info("Config loaded: effect={}, title={}, showModCount={}", config.getEffectType(), config.getWindowTitle(), config.isShowModCount());
                return config;
            } catch (IOException e) {
                TranslucentWindowClient.LOGGER.error("Failed to load config", e);
            }
        }

        TranslucentWindowClient.LOGGER.info("No config found, creating default");
        TranslucentWindowConfig config = new TranslucentWindowConfig();
        config.save();
        return config;
    }
}
