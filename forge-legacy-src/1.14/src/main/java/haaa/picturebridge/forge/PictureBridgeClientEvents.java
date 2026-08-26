package haaa.picturebridge.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.net.URI;

final class PictureBridgeClientEvents {
    @SubscribeEvent public void onClick(GuiScreenEvent.MouseClickedEvent.Pre event){
        if(!(event.getGui() instanceof ChatScreen)||event.getButton()!=0)return;Minecraft mc=Minecraft.getInstance();
        ITextComponent component=mc.ingameGUI.getChatGUI().getChatComponent((int)event.getMouseX(),(int)event.getMouseY());
        URI uri=component==null?null:ShitBotImageLink.find(component.getStyle());if(uri==null)return;
        mc.displayGuiScreen(new ImageViewerScreen((Screen)event.getGui(),uri));event.setCanceled(true);
    }
}
