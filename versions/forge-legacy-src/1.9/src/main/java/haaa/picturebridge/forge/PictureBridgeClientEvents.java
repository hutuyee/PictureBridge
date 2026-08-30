package haaa.picturebridge.forge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

import java.net.URI;

final class PictureBridgeClientEvents {
    @SubscribeEvent public void onMouse(GuiScreenEvent.MouseInputEvent.Pre event){
        if(!(event.getGui() instanceof GuiChat)||Mouse.getEventButton()!=0||!Mouse.getEventButtonState())return;
        Minecraft mc=Minecraft.getMinecraft();GuiScreen screen=event.getGui();int x=Mouse.getEventX()*screen.width/mc.displayWidth;int y=screen.height-Mouse.getEventY()*screen.height/mc.displayHeight-1;
        ITextComponent component=mc.ingameGUI.getChatGUI().getChatComponent(x,y);URI uri=component==null?null:ShitBotImageLink.find(component.getStyle());if(uri==null)return;
        mc.displayGuiScreen(new ImageViewerScreen(screen,uri));event.setCanceled(true);
    }
}
