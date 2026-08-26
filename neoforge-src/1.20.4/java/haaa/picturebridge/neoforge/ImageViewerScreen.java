package haaa.picturebridge.neoforge;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.net.URI;

public final class ImageViewerScreen extends BaseImageViewerScreen {
    public ImageViewerScreen(Screen parent, URI imageUri) {
        super(parent, imageUri);
    }

    @Override
    protected void blitTexture(GuiGraphics graphics, DrawnImage drawn) {
        AnimatedNeoTexture texture = picturebridgeTexture();
        graphics.blit(texture.location(), drawn.x(), drawn.y(), drawn.width(), drawn.height(),
                0.0F, 0.0F, texture.width(), texture.height(), texture.width(), texture.height());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return handleImageScroll(mouseX, mouseY, amount)
                || super.mouseScrolled(mouseX, mouseY, amount);
    }
}
