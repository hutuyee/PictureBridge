package haaa.picturebridge.forge;

import haaa.picturebridge.forge.common.DecodedImage;
import haaa.picturebridge.forge.common.ImageLoadException;
import haaa.picturebridge.forge.common.RemoteImageLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

final class ImageViewerScreen extends GuiScreen {
    private static final int IMAGE_MARGIN = 8;
    private static final int IMAGE_TOP = 25;
    private static final int FOOTER_HEIGHT = 51;
    private static final double MIN_ZOOM = 0.10;
    private static final double MAX_ZOOM = 12.0;

    private final GuiScreen parent;
    private final URI imageUri;
    private State state = State.LOADING;
    private AnimatedForgeTexture texture;
    private int generation;
    private boolean started;
    private boolean dragging;
    private int lastMouseX;
    private int lastMouseY;
    private long lastClickNanos;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private String errorText = "";
    private long copiedUntilNanos;

    ImageViewerScreen(GuiScreen parent, URI imageUri) {
        this.parent = parent;
        this.imageUri = imageUri;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int buttonWidth = Math.max(60, Math.min(96, (width - 32) / 3));
        int gap = 5;
        int startX = (width - buttonWidth * 3 - gap * 2) / 2;
        int buttonY = Math.max(0, height - 27);
        buttonList.add(new GuiButton(0, startX, buttonY, buttonWidth, 20, tr("picturebridge.button.back")));
        buttonList.add(new GuiButton(1, startX + buttonWidth + gap, buttonY, buttonWidth, 20,
                tr("picturebridge.button.reload")));
        buttonList.add(new GuiButton(2, startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20,
                tr("picturebridge.button.copy_url")));
        if (!started) {
            started = true;
            startLoad(false);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) mc.displayGuiScreen(parent);
        else if (button.id == 1) startLoad(true);
        else if (button.id == 2) {
            setClipboardString(imageUri.toASCIIString());
            copiedUntilNanos = System.nanoTime() + 2_000_000_000L;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xC0101115);
        ImageArea area = imageArea();
        drawRect(area.left, area.top, area.right, area.bottom, 0xB0101115);
        drawBorder(area, 0xFF3A3D46);

        if (state == State.READY && texture != null) drawImage(area);
        else if (state == State.ERROR) drawError(area);
        else drawCenteredString(fontRenderer, tr("picturebridge.status.loading", dots()),
                    area.centerX(), area.centerY() - 4, 0xD9E2F2);

        drawCenteredString(fontRenderer, tr("picturebridge.screen.title"), width / 2, 8, 0xFFFFFF);
        String status = System.nanoTime() < copiedUntilNanos
                ? tr("picturebridge.status.copied") : tr("picturebridge.status.hint");
        drawCenteredString(fontRenderer, status, width / 2, Math.max(0, height - 45),
                System.nanoTime() < copiedUntilNanos ? 0x72E49A : 0xA0A7B4);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawImage(ImageArea area) {
        texture.update(System.nanoTime());
        DrawnImage drawn = calculateDrawnImage(area);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        GL11.glScissor((area.left + 1) * scale, (height - area.bottom + 1) * scale,
                Math.max(1, area.width() - 2) * scale, Math.max(1, area.height() - 2) * scale);
        mc.getTextureManager().bindTexture(texture.location());
        GL11.glColor4f(1F, 1F, 1F, 1F);
        drawModalRectWithCustomSizedTexture(drawn.x, drawn.y, 0F, 0F, drawn.width, drawn.height,
                texture.width(), texture.height());
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        String dimensions = texture.animated()
                ? tr("picturebridge.status.ready_animated", texture.width(), texture.height(),
                        texture.frameCount(), Math.round(zoom * 100.0))
                : tr("picturebridge.status.ready", texture.width(), texture.height(), Math.round(zoom * 100.0));
        fontRenderer.drawStringWithShadow(dimensions, width - fontRenderer.getStringWidth(dimensions) - 8, 8,
                0xB8C7D9);
    }

    private void drawError(ImageArea area) {
        int contentWidth = Math.max(40, Math.min(420, area.width() - 24));
        int y = area.centerY() - 24;
        drawCenteredString(fontRenderer, tr("picturebridge.error.title"), area.centerX(), y, 0xFF6B6B);
        List<String> lines = fontRenderer.listFormattedStringToWidth(errorText, contentWidth);
        for (String line : lines) {
            drawCenteredString(fontRenderer, line, area.centerX(), y += 11, 0xD5D9E2);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (state == State.READY && mouseButton == 0 && imageArea().contains(mouseX, mouseY)) {
            long now = System.nanoTime();
            if (now - lastClickNanos <= 250_000_000L) resetView();
            else dragging = true;
            lastClickNanos = now;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (dragging && clickedMouseButton == 0 && state == State.READY) {
            panX += mouseX - lastMouseX;
            panY += mouseY - lastMouseY;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            clampPan(imageArea());
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        dragging = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || this.state != State.READY) return;
        int mouseX = Mouse.getEventX() * width / mc.displayWidth;
        int mouseY = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        ImageArea area = imageArea();
        if (!area.contains(mouseX, mouseY)) return;
        zoomAt(mouseX, mouseY, wheel > 0 ? 1.0 : -1.0, area);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (state == State.READY && keyCode == Keyboard.KEY_R) resetView();
        else super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        generation++;
        destroyTexture();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private void startLoad(boolean forceRefresh) {
        final int requestGeneration = ++generation;
        state = State.LOADING;
        errorText = "";
        resetView();
        destroyTexture();
        RemoteImageLoader.INSTANCE.loadImage(imageUri, forceRefresh).whenComplete((decoded, throwable) ->
                Minecraft.getMinecraft().addScheduledTask(() ->
                        finishLoad(requestGeneration, decoded, throwable)));
    }

    private void finishLoad(int requestGeneration, DecodedImage decoded, Throwable throwable) {
        if (requestGeneration != generation || mc.currentScreen != this) return;
        if (throwable != null || decoded == null) {
            state = State.ERROR;
            errorText = errorText(throwable);
            return;
        }
        texture = new AnimatedForgeTexture(mc, decoded);
        state = State.READY;
    }

    private void destroyTexture() {
        if (texture != null) texture.close();
        texture = null;
    }

    private void zoomAt(double mouseX, double mouseY, double amount, ImageArea area) {
        double oldScale = fittedScale(area) * zoom;
        double oldWidth = texture.width() * oldScale;
        double oldHeight = texture.height() * oldScale;
        double oldLeft = area.centerX() - oldWidth / 2.0 + panX;
        double oldTop = area.centerY() - oldHeight / 2.0 + panY;
        double relativeX = oldWidth <= 0 ? .5 : (mouseX - oldLeft) / oldWidth;
        double relativeY = oldHeight <= 0 ? .5 : (mouseY - oldTop) / oldHeight;
        zoom = clamp(zoom * Math.pow(1.2, amount), MIN_ZOOM, MAX_ZOOM);
        double newScale = fittedScale(area) * zoom;
        double newWidth = texture.width() * newScale;
        double newHeight = texture.height() * newScale;
        panX = mouseX - relativeX * newWidth - (area.centerX() - newWidth / 2.0);
        panY = mouseY - relativeY * newHeight - (area.centerY() - newHeight / 2.0);
        clampPan(area);
    }

    private DrawnImage calculateDrawnImage(ImageArea area) {
        clampPan(area);
        double scale = fittedScale(area) * zoom;
        return new DrawnImage((int) Math.round(area.centerX() - texture.width() * scale / 2 + panX),
                (int) Math.round(area.centerY() - texture.height() * scale / 2 + panY),
                Math.max(1, (int) Math.round(texture.width() * scale)),
                Math.max(1, (int) Math.round(texture.height() * scale)));
    }

    private double fittedScale(ImageArea area) {
        return Math.min(Math.max(1, area.width() - 4) / (double) texture.width(),
                Math.max(1, area.height() - 4) / (double) texture.height());
    }

    private void clampPan(ImageArea area) {
        if (texture == null) { panX = panY = 0; return; }
        double scale = fittedScale(area) * zoom;
        panX = clampAxis(panX, texture.width() * scale, area.width());
        panY = clampAxis(panY, texture.height() * scale, area.height());
    }

    private static double clampAxis(double value, double imageSize, double areaSize) {
        if (imageSize <= areaSize) return 0;
        double maximum = Math.max(0, (imageSize + areaSize) / 2 - 24);
        return clamp(value, -maximum, maximum);
    }

    private void resetView() { zoom = 1; panX = panY = 0; }

    private ImageArea imageArea() {
        int left = Math.min(IMAGE_MARGIN, Math.max(0, width / 4));
        int right = Math.max(left + 1, width - left);
        int top = Math.min(IMAGE_TOP, Math.max(0, height / 4));
        int bottom = Math.max(top + 1, height - FOOTER_HEIGHT);
        return new ImageArea(left, top, right, bottom);
    }

    private static String errorText(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ImageLoadException) {
            ImageLoadException error = (ImageLoadException) cause;
            return tr(error.translationKey(), error.arguments());
        }
        String message = cause == null || cause.getMessage() == null ? "unknown error" : cause.getMessage();
        return tr("picturebridge.error.network", message);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) current = current.getCause();
        return current;
    }

    private static String tr(String key, Object... arguments) { return I18n.format(key, arguments); }
    private static String dots() {
        int count = (int) (System.currentTimeMillis() / 350L % 4L);
        return count == 0 ? "" : count == 1 ? "." : count == 2 ? ".." : "...";
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private static void drawBorder(ImageArea area, int color) {
        drawRect(area.left, area.top, area.right, area.top + 1, color);
        drawRect(area.left, area.bottom - 1, area.right, area.bottom, color);
        drawRect(area.left, area.top, area.left + 1, area.bottom, color);
        drawRect(area.right - 1, area.top, area.right, area.bottom, color);
    }

    private enum State { LOADING, READY, ERROR }
    private static final class ImageArea {
        final int left, top, right, bottom;
        ImageArea(int left, int top, int right, int bottom) {
            this.left = left; this.top = top; this.right = right; this.bottom = bottom;
        }
        int width() { return right - left; }
        int height() { return bottom - top; }
        int centerX() { return left + width() / 2; }
        int centerY() { return top + height() / 2; }
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }
    private static final class DrawnImage {
        final int x, y, width, height;
        DrawnImage(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }
    }
}
