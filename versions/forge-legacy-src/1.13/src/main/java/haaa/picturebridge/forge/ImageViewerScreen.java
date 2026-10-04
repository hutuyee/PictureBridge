package haaa.picturebridge.forge;

import haaa.picturebridge.forge.common.DecodedImage;
import haaa.picturebridge.forge.common.ImageLoadException;
import haaa.picturebridge.forge.common.RemoteImageLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

final class ImageViewerScreen extends GuiScreen {
    private final GuiScreen parent;
    private final URI uri;
    private AnimatedForgeTexture texture;
    private String error = "";
    private int generation;
    private boolean started;
    private boolean dragging;
    private double zoom = 1;
    private double panX;
    private double panY;
    private int mouseX;
    private int mouseY;
    private long copied;
    private long lastClick;

    ImageViewerScreen(GuiScreen parent, URI uri) {
        this.parent = parent;
        this.uri = uri;
    }

    @Override
    protected void initGui() {
        int w = Math.max(60, Math.min(96, (width - 32) / 3));
        int x = (width - w * 3 - 10) / 2;
        int y = Math.max(0, height - 27);
        addAction(0, x, y, w, "picturebridge.button.back", this::close);
        addAction(1, x + w + 5, y, w, "picturebridge.button.reload", () -> load(true));
        addAction(2, x + (w + 5) * 2, y, w, "picturebridge.button.copy_url", this::copy);
        if (!started) {
            started = true;
            load(false);
        }
    }

    private void addAction(int id, int x, int y, int width, String key, Runnable action) {
        addButton(new GuiButton(id, x, y, width, 20, tr(key)) {
            @Override
            public void onClick(double mouseX, double mouseY) {
                action.run();
            }
        });
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        drawRect(0, 0, width, height, 0xC0101115);
        Area area = area();
        drawRect(area.left, area.top, area.right, area.bottom, 0xB0101115);
        border(area, 0xFF3A3D46);
        if (texture != null) {
            drawImage(area);
        } else {
            drawCenteredString(fontRenderer, error.isEmpty()
                    ? tr("picturebridge.status.loading", dots()) : error,
                    area.centerX(), area.centerY() - 4, error.isEmpty() ? 0xD9E2F2 : 0xFF6B6B);
        }
        drawCenteredString(fontRenderer, tr("picturebridge.screen.title"), width / 2, 8, 0xFFFFFF);
        drawCenteredString(fontRenderer, tr(System.nanoTime() < copied
                ? "picturebridge.status.copied" : "picturebridge.status.hint"),
                width / 2, Math.max(0, height - 45), 0xA0A7B4);
        super.render(mouseX, mouseY, partialTicks);
    }

    private void drawImage(Area area) {
        texture.update(System.nanoTime());
        double scale = Math.min((area.width() - 4D) / texture.width(),
                (area.height() - 4D) / texture.height()) * zoom;
        int imageWidth = Math.max(1, (int) Math.round(texture.width() * scale));
        int imageHeight = Math.max(1, (int) Math.round(texture.height() * scale));
        int x = (int) Math.round(area.centerX() - imageWidth / 2D + panX);
        int y = (int) Math.round(area.centerY() - imageHeight / 2D + panY);
        int guiScale = Math.max(1, (int) mc.mainWindow.getGuiScaleFactor());
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((area.left + 1) * guiScale, (height - area.bottom + 1) * guiScale,
                Math.max(1, area.width() - 2) * guiScale, Math.max(1, area.height() - 2) * guiScale);
        mc.getTextureManager().bindTexture(texture.location());
        GL11.glColor4f(1, 1, 1, 1);
        drawScaledCustomSizeModalRect(x, y, 0, 0, texture.width(), texture.height(),
                imageWidth, imageHeight, texture.width(), texture.height());
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        String status = texture.animated()
                ? tr("picturebridge.status.ready_animated", texture.width(), texture.height(),
                        texture.frames(), Math.round(zoom * 100))
                : tr("picturebridge.status.ready", texture.width(), texture.height(), Math.round(zoom * 100));
        fontRenderer.drawStringWithShadow(status, width - fontRenderer.getStringWidth(status) - 8, 8, 0xB8C7D9);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        if (texture != null && button == 0 && area().contains(x, y)) {
            long now = System.nanoTime();
            if (now - lastClick < 250_000_000L) reset();
            else dragging = true;
            lastClick = now;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragging && texture != null && button == 0) {
            panX += dx;
            panY += dy;
            return true;
        }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = false;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double amount) {
        if (texture != null && amount != 0 && area().contains(mouseX, mouseY)) {
            zoom = Math.max(.1, Math.min(12, zoom * Math.pow(1.2, amount)));
            return true;
        }
        return super.mouseScrolled(amount);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (texture != null && key == GLFW.GLFW_KEY_R) {
            reset();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void close() { mc.displayGuiScreen(parent); }
    @Override public boolean doesGuiPauseGame() { return false; }
    @Override public void onGuiClosed() { generation++; destroy(); super.onGuiClosed(); }

    private void load(boolean refresh) {
        final int request = ++generation;
        error = "";
        reset();
        destroy();
        RemoteImageLoader.INSTANCE.loadImage(uri, refresh).whenComplete((decoded, failure) ->
                Minecraft.getInstance().addScheduledTask(() -> finish(request, decoded, failure)));
    }

    private void finish(int request, DecodedImage decoded, Throwable failure) {
        if (request != generation) return;
        if (failure != null || decoded == null) error = errorText(failure);
        else texture = new AnimatedForgeTexture(mc, decoded);
    }

    private void copy() {
        mc.keyboardListener.setClipboardString(uri.toASCIIString());
        copied = System.nanoTime() + 2_000_000_000L;
    }

    private void destroy() { if (texture != null) texture.close(); texture = null; }
    private void reset() { zoom = 1; panX = panY = 0; }
    private Area area() { return new Area(8, 25, Math.max(9, width - 8), Math.max(26, height - 51)); }
    private static String tr(String key, Object... args) { return I18n.format(key, args); }

    private static String dots() {
        int count = (int) (System.currentTimeMillis() / 350 % 4);
        return count == 0 ? "" : count == 1 ? "." : count == 2 ? ".." : "...";
    }

    private static String errorText(Throwable failure) {
        Throwable cause = failure;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) cause = cause.getCause();
        if (cause instanceof ImageLoadException) {
            ImageLoadException error = (ImageLoadException) cause;
            return tr(error.translationKey(), error.arguments());
        }
        return tr("picturebridge.error.network", cause == null || cause.getMessage() == null
                ? "unknown error" : cause.getMessage());
    }

    private static void border(Area area, int color) {
        drawRect(area.left, area.top, area.right, area.top + 1, color);
        drawRect(area.left, area.bottom - 1, area.right, area.bottom, color);
        drawRect(area.left, area.top, area.left + 1, area.bottom, color);
        drawRect(area.right - 1, area.top, area.right, area.bottom, color);
    }

    private static final class Area {
        final int left, top, right, bottom;
        Area(int left, int top, int right, int bottom) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
        int width() { return right - left; }
        int height() { return bottom - top; }
        int centerX() { return left + width() / 2; }
        int centerY() { return top + height() / 2; }
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }
}
