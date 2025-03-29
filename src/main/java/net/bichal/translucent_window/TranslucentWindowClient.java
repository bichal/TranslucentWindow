package net.bichal.translucent_window;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.W32APIOptions;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@Environment(EnvType.CLIENT)
public class TranslucentWindowClient implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("Translucent Window");
    private int prevWidth = 0;
    private int prevHeight = 0;
    private boolean effectApplied = false;

    public interface DwmApi extends Library {
        DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

        int DWMWA_SYSTEMBACKDROP_TYPE = 38;
        int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
        int DWMWA_MICA_EFFECT = 1029;
        int DWMWA_CAPTION_COLOR = 35;
        int DWMWA_BORDER_COLOR = 34;
        int DWMWA_WINDOW_CORNER_PREFERENCE = 41;

        int DWMSBT_MAINWINDOW = 2;
        int DWMSBT_NONE = 1;

        int DWMWCP_ROUND = 2;

        void DwmSetWindowAttribute(HWND hwnd, int attribute, Object value, int valueSize);

        void DwmExtendFrameIntoClientArea(HWND hwnd, MARGINS margins);
    }

    public static class MARGINS extends Structure {
        public int cxLeftWidth;
        public int cxRightWidth;
        public int cyTopHeight;
        public int cyBottomHeight;

        public MARGINS(int left, int right, int top, int bottom) {
            this.cxLeftWidth = left;
            this.cxRightWidth = right;
            this.cyTopHeight = top;
            this.cyBottomHeight = bottom;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight");
        }
    }

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing translucent window mod...");

        TranslucentWindowConfig.getInstance();

        ClientLifecycleEvents.CLIENT_STARTED.register(this::applyTranslucentEffect);
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        if (client.getWindow() != null && effectApplied) {
            int currentWidth = client.getWindow().getWidth();
            int currentHeight = client.getWindow().getHeight();

            if (currentWidth != prevWidth || currentHeight != prevHeight) {
                LOGGER.info("Detected window size change: {}x{} -> {}x{}", prevWidth, prevHeight, currentWidth, currentHeight);
                reapplyTranslucentEffect(client);
                prevWidth = currentWidth;
                prevHeight = currentHeight;
            }
        }
    }

    public void applyTranslucentEffect(MinecraftClient client) {
        try {
            long windowHandle = client.getWindow().getHandle();
            HWND hwnd = new HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(windowHandle)));
            TranslucentWindowConfig config = TranslucentWindowConfig.getInstance();

            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DwmApi.DWMWA_USE_IMMERSIVE_DARK_MODE, new int[]{1}, 4);

            int backdropType = config.getDwmBackdropType();
            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DwmApi.DWMWA_SYSTEMBACKDROP_TYPE, new int[]{backdropType}, 4);

            DwmApi.INSTANCE.DwmExtendFrameIntoClientArea(hwnd, new MARGINS(0, 0, 1, 0));

            if (config.getEffectType() == TranslucentWindowConfig.EFFECT_MICA) {
                DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DwmApi.DWMWA_MICA_EFFECT, new int[]{1}, 4);
            }

            Color tintColor = config.getWindowTintColor();
            if (tintColor.getAlpha() > 0) {
                int colorValue = (tintColor.getAlpha() << 24) | (tintColor.getRed() << 16) | (tintColor.getGreen() << 8) | tintColor.getBlue();
                DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DwmApi.DWMWA_CAPTION_COLOR, new int[]{colorValue}, 4);
                DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DwmApi.DWMWA_BORDER_COLOR, new int[]{colorValue}, 4);
            }

            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DwmApi.DWMWA_WINDOW_CORNER_PREFERENCE, new int[]{DwmApi.DWMWCP_ROUND}, 4);

            effectApplied = true;
            prevWidth = client.getWindow().getWidth();
            prevHeight = client.getWindow().getHeight();

            LOGGER.info("Translucent effect applied successfully");
        } catch (Exception e) {
            LOGGER.error("Error applying translucent effect", e);
        }
    }

    public void reapplyTranslucentEffect(MinecraftClient client) {
        try {
            long windowHandle = client.getWindow().getHandle();
            HWND hwnd = new HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(windowHandle)));

            DwmApi.INSTANCE.DwmSetWindowAttribute(hwnd, DwmApi.DWMWA_SYSTEMBACKDROP_TYPE, new int[]{DwmApi.DWMSBT_NONE}, 4);

            applyTranslucentEffect(client);
        } catch (Exception e) {
            LOGGER.error("Error reapplying translucent effect", e);
        }
    }
}
