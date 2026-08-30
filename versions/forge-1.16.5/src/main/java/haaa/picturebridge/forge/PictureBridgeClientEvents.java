package haaa.picturebridge.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.net.URI;

final class PictureBridgeClientEvents {
    @SubscribeEvent
    public void onMouseClicked(GuiScreenEvent.MouseClickedEvent.Pre event) {
        if (!(event.getGui() instanceof ChatScreen) || event.getButton() != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        Style style = minecraft.gui.getChat().getClickedComponentStyleAt(event.getMouseX(), event.getMouseY());
        URI uri = ShitBotImageLink.find(style);
        if (uri == null) return;
        minecraft.setScreen(new ImageViewerScreen(event.getGui(), uri));
        event.setCanceled(true);
    }
}
