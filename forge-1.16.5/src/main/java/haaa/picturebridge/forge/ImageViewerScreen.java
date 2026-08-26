package haaa.picturebridge.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import haaa.picturebridge.forge.common.DecodedImage;
import haaa.picturebridge.forge.common.ImageLoadException;
import haaa.picturebridge.forge.common.RemoteImageLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

final class ImageViewerScreen extends Screen {
    private static final int IMAGE_MARGIN = 8;
    private static final int IMAGE_TOP = 25;
    private static final int FOOTER_HEIGHT = 51;
    private static final double MIN_ZOOM = 0.10;
    private static final double MAX_ZOOM = 12.0;

    private final Screen parent;
    private final URI imageUri;
    private State state = State.LOADING;
    private AnimatedForgeTexture texture;
    private int generation;
    private boolean started;
    private boolean dragging;
    private long lastClickNanos;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private String errorText = "";
    private long copiedUntilNanos;

    ImageViewerScreen(Screen parent, URI imageUri) {
        super(new TranslatableComponent("picturebridge.screen.title"));
        this.parent = parent;
        this.imageUri = imageUri;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.max(60, Math.min(96, (width - 32) / 3));
        int gap = 5;
        int startX = (width - buttonWidth * 3 - gap * 2) / 2;
        int buttonY = Math.max(0, height - 27);
        addButton(new Button(startX, buttonY, buttonWidth, 20,
                new TranslatableComponent("picturebridge.button.back"), button -> onClose()));
        addButton(new Button(startX + buttonWidth + gap, buttonY, buttonWidth, 20,
                new TranslatableComponent("picturebridge.button.reload"), button -> startLoad(true)));
        addButton(new Button(startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20,
                new TranslatableComponent("picturebridge.button.copy_url"), button -> copyUrl()));
        if (!started) {
            started = true;
            startLoad(false);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTicks) {
        fill(poseStack, 0, 0, width, height, 0xC0101115);
        ImageArea area = imageArea();
        fill(poseStack, area.left, area.top, area.right, area.bottom, 0xB0101115);
        drawBorder(poseStack, area, 0xFF3A3D46);
        if (state == State.READY && texture != null) drawImage(poseStack, area);
        else if (state == State.ERROR) drawError(poseStack, area);
        else drawCenteredString(poseStack, font, tr("picturebridge.status.loading", dots()),
                    area.centerX(), area.centerY() - 4, 0xD9E2F2);

        drawCenteredString(poseStack, font, title, width / 2, 8, 0xFFFFFF);
        boolean copied = System.nanoTime() < copiedUntilNanos;
        drawCenteredString(poseStack, font,
                tr(copied ? "picturebridge.status.copied" : "picturebridge.status.hint"),
                width / 2, Math.max(0, height - 45), copied ? 0x72E49A : 0xA0A7B4);
        super.render(poseStack, mouseX, mouseY, partialTicks);
    }

    private void drawImage(PoseStack poseStack, ImageArea area) {
        texture.update(System.nanoTime());
        DrawnImage drawn = calculateDrawnImage(area);
        int scale = Math.max(1, (int) Math.round(minecraft.getWindow().getGuiScale()));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((area.left + 1) * scale, (height - area.bottom + 1) * scale,
                Math.max(1, area.width() - 2) * scale, Math.max(1, area.height() - 2) * scale);
        minecraft.getTextureManager().bind(texture.location());
        RenderSystem.color4f(1F, 1F, 1F, 1F);
        blit(poseStack, drawn.x, drawn.y, 0F, 0F, drawn.width, drawn.height,
                texture.width(), texture.height());
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        String dimensions = texture.animated()
                ? tr("picturebridge.status.ready_animated", texture.width(), texture.height(),
                        texture.frameCount(), Math.round(zoom * 100.0))
                : tr("picturebridge.status.ready", texture.width(), texture.height(), Math.round(zoom * 100.0));
        font.drawShadow(poseStack, dimensions, width - font.width(dimensions) - 8, 8, 0xB8C7D9);
    }

    private void drawError(PoseStack poseStack, ImageArea area) {
        int contentWidth = Math.max(40, Math.min(420, area.width() - 24));
        int y = area.centerY() - 24;
        drawCenteredString(poseStack, font, tr("picturebridge.error.title"), area.centerX(), y, 0xFF6B6B);
        List<FormattedCharSequence> lines = font.split(new net.minecraft.network.chat.TextComponent(errorText),
                contentWidth);
        for (FormattedCharSequence line : lines) {
            font.drawShadow(poseStack, line, area.centerX() - font.width(line) / 2.0F, y += 11, 0xD5D9E2);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (state == State.READY && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && imageArea().contains(mouseX, mouseY)) {
            long now = System.nanoTime();
            if (now - lastClickNanos <= 250_000_000L) resetView();
            else dragging = true;
            lastClickNanos = now;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && state == State.READY && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            panX += deltaX;
            panY += deltaY;
            clampPan(imageArea());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = dragging;
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button) || wasDragging;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        ImageArea area = imageArea();
        if (state != State.READY || !area.contains(mouseX, mouseY) || amount == 0) {
            return super.mouseScrolled(mouseX, mouseY, amount);
        }
        zoomAt(mouseX, mouseY, amount, area);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (state == State.READY && keyCode == GLFW.GLFW_KEY_R) {
            resetView();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        generation++;
        destroyTexture();
        super.removed();
    }

    private void startLoad(boolean forceRefresh) {
        final int requestGeneration = ++generation;
        state = State.LOADING;
        errorText = "";
        resetView();
        destroyTexture();
        RemoteImageLoader.INSTANCE.loadImage(imageUri, forceRefresh).whenComplete((decoded, throwable) ->
                Minecraft.getInstance().execute(() -> finishLoad(requestGeneration, decoded, throwable)));
    }

    private void finishLoad(int requestGeneration, DecodedImage decoded, Throwable throwable) {
        if (requestGeneration != generation || minecraft.screen != this) return;
        if (throwable != null || decoded == null) {
            state = State.ERROR;
            errorText = errorText(throwable);
            return;
        }
        texture = new AnimatedForgeTexture(minecraft, decoded);
        state = State.READY;
    }

    private void copyUrl() {
        minecraft.keyboardHandler.setClipboard(imageUri.toASCIIString());
        copiedUntilNanos = System.nanoTime() + 2_000_000_000L;
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

    private static String tr(String key, Object... arguments) { return I18n.get(key, arguments); }
    private static String dots() {
        int count = (int) (System.currentTimeMillis() / 350L % 4L);
        return count == 0 ? "" : count == 1 ? "." : count == 2 ? ".." : "...";
    }
    private static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }

    private static void drawBorder(PoseStack poseStack, ImageArea area, int color) {
        fill(poseStack, area.left, area.top, area.right, area.top + 1, color);
        fill(poseStack, area.left, area.bottom - 1, area.right, area.bottom, color);
        fill(poseStack, area.left, area.top, area.left + 1, area.bottom, color);
        fill(poseStack, area.right - 1, area.top, area.right, area.bottom, color);
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
