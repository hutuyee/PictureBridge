package haaa.picturebridge.fabric.legacy;

import com.mojang.blaze3d.systems.RenderSystem;
import haaa.picturebridge.forge.common.DecodedImage;
import haaa.picturebridge.forge.common.ImageLoadException;
import haaa.picturebridge.forge.common.RemoteImageLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.TranslatableText;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class ImageViewerScreen extends Screen {
    private final Screen parent;
    private final URI uri;
    private AnimatedFabricTexture texture;
    private String error = "";
    private int generation;
    private boolean started;
    private boolean dragging;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private long copiedUntil;
    private long lastClick;

    public ImageViewerScreen(Screen parent, URI uri) {
        super(new TranslatableText("picturebridge.screen.title"));
        this.parent = parent;
        this.uri = uri;
    }

    @Override
    protected void init() {
        int w = Math.max(60, Math.min(96, (width - 32) / 3));
        int x = (width - w * 3 - 10) / 2;
        int y = Math.max(0, height - 27);
        addButton(new ButtonWidget(x, y, w, 20, tr("picturebridge.button.back"), b -> onClose()));
        addButton(new ButtonWidget(x + w + 5, y, w, 20, tr("picturebridge.button.reload"), b -> load(true)));
        addButton(new ButtonWidget(x + (w + 5) * 2, y, w, 20, tr("picturebridge.button.copy_url"), b -> copyUrl()));
        if (!started) { started = true; load(false); }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        fill(0, 0, width, height, 0xC0101115);
        Area area = area();
        fill(area.left, area.top, area.right, area.bottom, 0xB0101115);
        border(area, 0xFF3A3D46);
        if (texture != null) drawImage(area);
        else drawCenteredString(textRenderer, error.isEmpty() ? tr("picturebridge.status.loading", dots()) : error,
                area.cx(), area.cy() - 4, error.isEmpty() ? 0xD9E2F2 : 0xFF6B6B);
        drawCenteredString(textRenderer, tr("picturebridge.screen.title"), width / 2, 8, 0xFFFFFF);
        drawCenteredString(textRenderer, tr(System.nanoTime() < copiedUntil
                        ? "picturebridge.status.copied" : "picturebridge.status.hint"),
                width / 2, Math.max(0, height - 45), 0xA0A7B4);
        super.render(mouseX, mouseY, delta);
    }

    private void drawImage(Area area) {
        texture.update(System.nanoTime());
        double scale = Math.min((area.width() - 4D) / texture.width(), (area.height() - 4D) / texture.height()) * zoom;
        int w = Math.max(1, (int) Math.round(texture.width() * scale));
        int h = Math.max(1, (int) Math.round(texture.height() * scale));
        int x = (int) Math.round(area.cx() - w / 2D + panX);
        int y = (int) Math.round(area.cy() - h / 2D + panY);
        int guiScale = Math.max(1, (int) client.getWindow().getScaleFactor());
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((area.left + 1) * guiScale, (height - area.bottom + 1) * guiScale,
                Math.max(1, area.width() - 2) * guiScale, Math.max(1, area.height() - 2) * guiScale);
        client.getTextureManager().bindTexture(texture.identifier());
        RenderSystem.color4f(1F, 1F, 1F, 1F);
        blit(x, y, 0F, 0F, w, h, texture.width(), texture.height());
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        String status = texture.animated()
                ? tr("picturebridge.status.ready_animated", texture.width(), texture.height(), texture.frameCount(), Math.round(zoom * 100))
                : tr("picturebridge.status.ready", texture.width(), texture.height(), Math.round(zoom * 100));
        textRenderer.drawWithShadow(status, width - textRenderer.getStringWidth(status) - 8, 8, 0xB8C7D9);
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
        if (dragging && texture != null && button == 0) { panX += dx; panY += dy; return true; }
        return super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        dragging = false;
        return super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double amount) {
        if (texture != null && amount != 0 && area().contains(x, y)) {
            zoom = Math.max(.1, Math.min(12, zoom * Math.pow(1.2, amount)));
            return true;
        }
        return super.mouseScrolled(x, y, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (texture != null && keyCode == GLFW.GLFW_KEY_R) { reset(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        destroyTexture();
        client.openScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void load(boolean refresh) {
        final int request = ++generation;
        error = "";
        reset();
        destroyTexture();
        RemoteImageLoader.INSTANCE.loadImage(uri, refresh).whenComplete((decoded, throwable) ->
                client.execute(() -> finish(request, decoded, throwable)));
    }

    private void finish(int request, DecodedImage decoded, Throwable throwable) {
        if (request != generation) return;
        if (throwable != null || decoded == null) error = errorText(throwable);
        else texture = new AnimatedFabricTexture(client, decoded);
    }

    private void copyUrl() {
        client.keyboard.setClipboard(uri.toASCIIString());
        copiedUntil = System.nanoTime() + 2_000_000_000L;
    }

    private void destroyTexture() { if (texture != null) texture.close(); texture = null; }
    private void reset() { zoom = 1; panX = panY = 0; }
    private Area area() { return new Area(8, 25, Math.max(9, width - 8), Math.max(26, height - 51)); }
    private static String tr(String key, Object... args) { return I18n.translate(key, args); }
    private static String dots() { int n = (int) (System.currentTimeMillis() / 350 % 4); return n == 0 ? "" : n == 1 ? "." : n == 2 ? ".." : "..."; }

    private static String errorText(Throwable throwable) {
        Throwable cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) cause = cause.getCause();
        if (cause instanceof ImageLoadException) {
            ImageLoadException imageError = (ImageLoadException) cause;
            return tr(imageError.translationKey(), imageError.arguments());
        }
        return tr("picturebridge.error.network", cause == null || cause.getMessage() == null ? "unknown error" : cause.getMessage());
    }

    private static void border(Area a, int color) {
        fill(a.left, a.top, a.right, a.top + 1, color); fill(a.left, a.bottom - 1, a.right, a.bottom, color);
        fill(a.left, a.top, a.left + 1, a.bottom, color); fill(a.right - 1, a.top, a.right, a.bottom, color);
    }

    private static final class Area {
        final int left, top, right, bottom;
        Area(int left, int top, int right, int bottom) { this.left = left; this.top = top; this.right = right; this.bottom = bottom; }
        int width() { return right - left; } int height() { return bottom - top; }
        int cx() { return left + width() / 2; } int cy() { return top + height() / 2; }
        boolean contains(double x, double y) { return x >= left && x < right && y >= top && y < bottom; }
    }
}
