package haaa.picturebridge.fabric.client;

import haaa.picturebridge.fabric.PictureBridgeClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class ImageViewerScreen extends Screen {
    private static final int IMAGE_MARGIN = 8;
    private static final int IMAGE_TOP = 25;
    private static final int FOOTER_HEIGHT = 51;
    private static final double MIN_ZOOM = 0.10;
    private static final double MAX_ZOOM = 12.0;

    private final Screen parent;
    private final URI imageUri;
    private State state = State.LOADING;
    private AnimatedTexture texture;
    private int imageWidth;
    private int imageHeight;
    private int generation;
    private boolean started;
    private boolean draggingImage;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private Text errorText = Text.empty();
    private long copiedUntilNanos;
    private long lastImageClickNanos;

    public ImageViewerScreen(Screen parent, URI imageUri) {
        super(Text.translatable("picturebridge.screen.title"));
        this.parent = parent;
        this.imageUri = imageUri;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.max(60, Math.min(96, (width - 32) / 3));
        int gap = 5;
        int totalWidth = buttonWidth * 3 + gap * 2;
        int startX = (width - totalWidth) / 2;
        int buttonY = Math.max(0, height - 27);

        addDrawableChild(ButtonWidget.builder(Text.translatable("picturebridge.button.back"), button -> close())
                .dimensions(startX, buttonY, buttonWidth, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("picturebridge.button.reload"), button -> startLoad(true))
                .dimensions(startX + buttonWidth + gap, buttonY, buttonWidth, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("picturebridge.button.copy_url"), button -> copyUrl())
                .dimensions(startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20)
                .build());

        if (!started) {
            started = true;
            startLoad(false);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // ChatScreen can open this screen during its own render pass. Calling
        // Screen#renderBackground here would then request a second blur in the same
        // frame, which Minecraft 1.20.1 rejects with "Can only blur once per frame".
        // A translucent fill keeps the world readable and is safe during that handoff.
        context.fill(0, 0, width, height, 0xC0101115);

        ImageArea area = imageArea();
        context.fill(area.left, area.top, area.right, area.bottom, 0xB0101115);
        drawBorder(context, area, 0xFF3A3D46);

        if (state == State.READY && texture != null) {
            renderImage(context, area);
        } else if (state == State.ERROR) {
            renderError(context, area);
        } else {
            renderLoading(context, area);
        }

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF);
        renderStatus(context);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderImage(DrawContext context, ImageArea area) {
        texture.update(System.nanoTime());
        DrawnImage drawn = calculateDrawnImage(area);
        context.enableScissor(area.left + 1, area.top + 1, area.right - 1, area.bottom - 1);
        context.drawTexture(
                texture.id(),
                drawn.x,
                drawn.y,
                drawn.width,
                drawn.height,
                0.0F,
                0.0F,
                imageWidth,
                imageHeight,
                imageWidth,
                imageHeight
        );
        context.disableScissor();

        Text dimensions = texture.animated()
                ? Text.translatable("picturebridge.status.ready_animated",
                imageWidth, imageHeight, texture.frameCount(), Math.round(zoom * 100.0))
                : Text.translatable("picturebridge.status.ready",
                imageWidth, imageHeight, Math.round(zoom * 100.0));
        context.drawTextWithShadow(
                textRenderer, dimensions, width - textRenderer.getWidth(dimensions) - 8, 8, 0xB8C7D9);
    }

    private void renderLoading(DrawContext context, ImageArea area) {
        int dotCount = (int) (System.currentTimeMillis() / 350L % 4L);
        String dots = ".".repeat(dotCount);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("picturebridge.status.loading", dots),
                area.centerX(),
                area.centerY() - 4,
                0xD9E2F2
        );
    }

    private void renderError(DrawContext context, ImageArea area) {
        int contentWidth = Math.max(40, Math.min(420, area.width() - 24));
        int startX = area.centerX() - contentWidth / 2;
        int startY = area.centerY() - 24;
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("picturebridge.error.title"),
                area.centerX(),
                startY,
                0xFF6B6B
        );
        int lineY = startY + 18;
        for (OrderedText line : textRenderer.wrapLines(errorText, contentWidth)) {
            context.drawTextWithShadow(textRenderer, line, startX, lineY, 0xD5D9E2);
            lineY += textRenderer.fontHeight + 1;
        }
    }

    private void renderStatus(DrawContext context) {
        int y = Math.max(0, height - 45);
        Text status;
        int color;
        if (System.nanoTime() < copiedUntilNanos) {
            status = Text.translatable("picturebridge.status.copied");
            color = 0x72E49A;
        } else {
            status = Text.translatable("picturebridge.status.hint");
            color = 0xA0A7B4;
        }
        context.drawCenteredTextWithShadow(textRenderer, status, width / 2, y, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (state == State.READY && button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && imageArea().contains(mouseX, mouseY)) {
            long now = System.nanoTime();
            if (now - lastImageClickNanos <= 250_000_000L) {
                resetView();
            } else {
                draggingImage = true;
            }
            lastImageClickNanos = now;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX,
                                double mouseY,
                                int button,
                                double offsetX,
                                double offsetY) {
        if (draggingImage && state == State.READY && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            panX += offsetX;
            panY += offsetY;
            clampPan(imageArea());
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = draggingImage;
        draggingImage = false;
        return super.mouseReleased(mouseX, mouseY, button) || wasDragging;
    }

    @Override
    public boolean mouseScrolled(double mouseX,
                                 double mouseY,
                                 double amount) {
        ImageArea area = imageArea();
        if (state != State.READY || !area.contains(mouseX, mouseY) || amount == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, amount);
        }

        double oldScale = fittedScale(area) * zoom;
        double oldWidth = imageWidth * oldScale;
        double oldHeight = imageHeight * oldScale;
        double oldLeft = area.centerX() - oldWidth / 2.0 + panX;
        double oldTop = area.centerY() - oldHeight / 2.0 + panY;
        double relativeX = oldWidth <= 0.0 ? 0.5 : (mouseX - oldLeft) / oldWidth;
        double relativeY = oldHeight <= 0.0 ? 0.5 : (mouseY - oldTop) / oldHeight;

        zoom = MathHelper.clamp(zoom * Math.pow(1.2, amount), MIN_ZOOM, MAX_ZOOM);
        double newScale = fittedScale(area) * zoom;
        double newWidth = imageWidth * newScale;
        double newHeight = imageHeight * newScale;
        panX = mouseX - relativeX * newWidth - (area.centerX() - newWidth / 2.0);
        panY = mouseY - relativeY * newHeight - (area.centerY() - newHeight / 2.0);
        clampPan(area);
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
        draggingImage = false;
        destroyTexture();
        super.removed();
    }

    private void startLoad(boolean forceRefresh) {
        int requestGeneration = ++generation;
        state = State.LOADING;
        errorText = Text.empty();
        resetView();
        destroyTexture();

        RemoteImageLoader.INSTANCE.loadImage(imageUri, forceRefresh).whenComplete((decoded, throwable) -> {
            MinecraftClient minecraft = MinecraftClient.getInstance();
            minecraft.execute(() -> finishLoad(requestGeneration, decoded, throwable));
        });
    }

    private void finishLoad(int requestGeneration,
                            RemoteImageLoader.DecodedImage decoded,
                            Throwable throwable) {
        if (requestGeneration != generation || client == null || client.currentScreen != this) {
            if (decoded != null) {
                decoded.close();
            }
            return;
        }

        if (throwable != null || decoded == null) {
            state = State.ERROR;
            errorText = toErrorText(throwable);
            return;
        }

        Identifier id = Identifier.of(
                PictureBridgeClient.MOD_ID,
                "remote/" + urlHash(imageUri) + "_" + requestGeneration);
        try {
            texture = AnimatedTexture.register(client, id, decoded);
            imageWidth = decoded.width();
            imageHeight = decoded.height();
            state = State.READY;
        } catch (RuntimeException exception) {
            texture = null;
            state = State.ERROR;
            errorText = Text.translatable("picturebridge.error.decode");
            PictureBridgeClient.LOGGER.warn("Failed to create texture for {}", imageUri, exception);
        }
    }

    private void copyUrl() {
        if (client != null) {
            client.keyboard.setClipboard(imageUri.toASCIIString());
            copiedUntilNanos = System.nanoTime() + 2_000_000_000L;
        }
    }

    private void resetView() {
        zoom = 1.0;
        panX = 0.0;
        panY = 0.0;
    }

    private void destroyTexture() {
        if (texture != null) {
            try {
                texture.close();
            } catch (RuntimeException exception) {
                PictureBridgeClient.LOGGER.debug("Failed to destroy image texture for {}", imageUri, exception);
            }
        }
        texture = null;
        imageWidth = 0;
        imageHeight = 0;
    }

    private DrawnImage calculateDrawnImage(ImageArea area) {
        clampPan(area);
        double scale = fittedScale(area) * zoom;
        int drawnWidth = Math.max(1, (int) Math.round(imageWidth * scale));
        int drawnHeight = Math.max(1, (int) Math.round(imageHeight * scale));
        int x = (int) Math.round(area.centerX() - drawnWidth / 2.0 + panX);
        int y = (int) Math.round(area.centerY() - drawnHeight / 2.0 + panY);
        return new DrawnImage(x, y, drawnWidth, drawnHeight);
    }

    private double fittedScale(ImageArea area) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return 1.0;
        }
        return Math.min(
                Math.max(1, area.width() - 4) / (double) imageWidth,
                Math.max(1, area.height() - 4) / (double) imageHeight);
    }

    private void clampPan(ImageArea area) {
        if (imageWidth <= 0 || imageHeight <= 0) {
            panX = 0.0;
            panY = 0.0;
            return;
        }
        double scale = fittedScale(area) * zoom;
        double drawnWidth = imageWidth * scale;
        double drawnHeight = imageHeight * scale;
        panX = clampAxis(panX, drawnWidth, area.width());
        panY = clampAxis(panY, drawnHeight, area.height());
    }

    private static double clampAxis(double value, double imageSize, double areaSize) {
        if (imageSize <= areaSize) {
            return 0.0;
        }
        double maximum = Math.max(0.0, (imageSize + areaSize) / 2.0 - 24.0);
        return MathHelper.clamp(value, -maximum, maximum);
    }

    private ImageArea imageArea() {
        int left = Math.min(IMAGE_MARGIN, Math.max(0, width / 4));
        int right = Math.max(left + 1, width - left);
        int top = Math.min(IMAGE_TOP, Math.max(0, height / 4));
        int bottom = Math.max(top + 1, height - FOOTER_HEIGHT);
        return new ImageArea(left, top, right, bottom);
    }

    private static void drawBorder(DrawContext context, ImageArea area, int color) {
        context.fill(area.left, area.top, area.right, area.top + 1, color);
        context.fill(area.left, area.bottom - 1, area.right, area.bottom, color);
        context.fill(area.left, area.top, area.left + 1, area.bottom, color);
        context.fill(area.right - 1, area.top, area.right, area.bottom, color);
    }

    private static Text toErrorText(Throwable throwable) {
        Throwable cause = unwrap(throwable);
        if (cause instanceof ImageLoadException imageLoadException) {
            return Text.translatable(imageLoadException.translationKey(), imageLoadException.arguments());
        }
        String message = cause == null || cause.getMessage() == null || cause.getMessage().isBlank()
                ? "unknown error"
                : cause.getMessage();
        return Text.translatable("picturebridge.error.network", message);
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String urlHash(URI uri) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(uri.toASCIIString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private enum State {
        LOADING,
        READY,
        ERROR
    }

    private record ImageArea(int left, int top, int right, int bottom) {
        int width() {
            return right - left;
        }

        int height() {
            return bottom - top;
        }

        int centerX() {
            return left + width() / 2;
        }

        int centerY() {
            return top + height() / 2;
        }

        boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    private record DrawnImage(int x, int y, int width, int height) {
    }
}
