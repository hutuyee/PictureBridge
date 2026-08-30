package haaa.picturebridge.neoforge;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;

import java.net.URI;

public final class ImageViewerScreen extends BaseImageViewerScreen {
    public ImageViewerScreen(Screen parent, URI imageUri) {
        super(parent, imageUri);
    }

    @Override
    protected void blitTexture(GuiGraphics graphics, DrawnImage drawn) {
        AnimatedNeoTexture texture = picturebridgeTexture();
        graphics.blit(RenderType::guiTextured, texture.location(),
                drawn.x(), drawn.y(), 0.0F, 0.0F, drawn.width(), drawn.height(),
                texture.width(), texture.height());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button)
                || handleLegacyImageClick(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return handleImageDrag(button, deltaX, deltaY)
                || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseReleased(mouseX, mouseY, button);
        return handleImageRelease() || handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return handleResetKey(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        return handleImageScroll(mouseX, mouseY, verticalAmount)
                || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
