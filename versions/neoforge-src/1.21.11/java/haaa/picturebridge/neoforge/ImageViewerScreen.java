package haaa.picturebridge.neoforge;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;

import java.net.URI;

public final class ImageViewerScreen extends BaseImageViewerScreen {
    public ImageViewerScreen(Screen parent, URI imageUri) {
        super(parent, imageUri);
    }

    @Override
    protected void blitTexture(GuiGraphics graphics, DrawnImage drawn) {
        AnimatedNeoTexture texture = picturebridgeTexture();
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture.location(),
                drawn.x(), drawn.y(), 0.0F, 0.0F, drawn.width(), drawn.height(),
                texture.width(), texture.height(), texture.width(), texture.height());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        return super.mouseClicked(click, doubled)
                || handleImageClick(click.x(), click.y(), click.button(), doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        return handleImageDrag(click.button(), deltaX, deltaY)
                || super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        boolean handled = super.mouseReleased(click);
        return handleImageRelease() || handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        return handleImageScroll(mouseX, mouseY, verticalAmount)
                || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        return handleResetKey(input.key()) || super.keyPressed(input);
    }
}
