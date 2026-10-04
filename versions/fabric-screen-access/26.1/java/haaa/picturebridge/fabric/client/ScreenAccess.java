package haaa.picturebridge.fabric.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenAccess {
    private ScreenAccess() {}

    public static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.setScreen(screen);
    }

    public static boolean isCurrent(Minecraft minecraft, Screen screen) {
        return minecraft.screen == screen;
    }
}
