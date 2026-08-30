package haaa.picturebridge.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.net.URI;

final class PictureBridgeClientEvents {
    @SubscribeEvent
    public void onMouseInput(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!(event.gui instanceof GuiChat) || Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        GuiScreen screen = event.gui;
        int mouseX = Mouse.getEventX() * screen.width / minecraft.displayWidth;
        int mouseY = screen.height - Mouse.getEventY() * screen.height / minecraft.displayHeight - 1;
        IChatComponent component = minecraft.ingameGUI.getChatGUI().getChatComponent(mouseX, mouseY);
        URI uri = component == null ? null : ShitBotImageLink.find(component.getChatStyle());
        if (uri == null) return;

        minecraft.displayGuiScreen(new ImageViewerScreen(screen, uri));
        event.setCanceled(true);
    }
}
