package haaa.picturebridge.fabric.legacy;

import com.mojang.blaze3d.systems.RenderSystem;
import haaa.picturebridge.forge.common.DecodedImage;
import haaa.picturebridge.forge.common.ImageLoadException;
import haaa.picturebridge.forge.common.RemoteImageLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
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
    private double zoom = 1, panX, panY;
    private long copiedUntil, lastClick;

    public ImageViewerScreen(Screen parent, URI uri) {
        super(new TranslatableText("picturebridge.screen.title"));
        this.parent = parent; this.uri = uri;
    }

    @Override protected void init() {
        int w = Math.max(60, Math.min(96, (width - 32) / 3)), x = (width - w * 3 - 10) / 2, y = Math.max(0, height - 27);
        addButton(new ButtonWidget(x, y, w, 20, new TranslatableText("picturebridge.button.back"), b -> client.openScreen(parent)));
        addButton(new ButtonWidget(x + w + 5, y, w, 20, new TranslatableText("picturebridge.button.reload"), b -> load(true)));
        addButton(new ButtonWidget(x + (w + 5) * 2, y, w, 20, new TranslatableText("picturebridge.button.copy_url"), b -> copyUrl()));
        if (!started) { started = true; load(false); }
    }

    @Override public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        fill(matrices, 0, 0, width, height, 0xC0101115);
        Area a = area(); fill(matrices, a.left, a.top, a.right, a.bottom, 0xB0101115); border(matrices, a, 0xFF3A3D46);
        if (texture != null) drawImage(matrices, a);
        else drawCenteredText(matrices, textRenderer, error.isEmpty() ? tr("picturebridge.status.loading", dots()) : error,
                a.cx(), a.cy() - 4, error.isEmpty() ? 0xD9E2F2 : 0xFF6B6B);
        drawCenteredText(matrices, textRenderer, title, width / 2, 8, 0xFFFFFF);
        drawCenteredText(matrices, textRenderer, tr(System.nanoTime() < copiedUntil ? "picturebridge.status.copied" : "picturebridge.status.hint"),
                width / 2, Math.max(0, height - 45), 0xA0A7B4);
        super.render(matrices, mouseX, mouseY, delta);
    }

    private void drawImage(MatrixStack matrices, Area a) {
        texture.update(System.nanoTime());
        double scale = Math.min((a.width() - 4D) / texture.width(), (a.height() - 4D) / texture.height()) * zoom;
        int w = Math.max(1, (int) Math.round(texture.width() * scale)), h = Math.max(1, (int) Math.round(texture.height() * scale));
        int x = (int) Math.round(a.cx() - w / 2D + panX), y = (int) Math.round(a.cy() - h / 2D + panY);
        int guiScale = Math.max(1, (int) client.getWindow().getScaleFactor());
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((a.left + 1) * guiScale, (height - a.bottom + 1) * guiScale,
                Math.max(1, a.width() - 2) * guiScale, Math.max(1, a.height() - 2) * guiScale);
        client.getTextureManager().bindTexture(texture.identifier()); RenderSystem.color4f(1, 1, 1, 1);
        drawTexture(matrices, x, y, 0F, 0F, w, h, texture.width(), texture.height());
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        String status = texture.animated()
                ? tr("picturebridge.status.ready_animated", texture.width(), texture.height(), texture.frameCount(), Math.round(zoom * 100))
                : tr("picturebridge.status.ready", texture.width(), texture.height(), Math.round(zoom * 100));
        textRenderer.drawWithShadow(matrices, status, width - textRenderer.getWidth(status) - 8, 8, 0xB8C7D9);
    }

    @Override public boolean mouseClicked(double x, double y, int b) {
        if (super.mouseClicked(x, y, b)) return true;
        if (texture != null && b == 0 && area().contains(x, y)) { long now = System.nanoTime(); if (now - lastClick < 250_000_000L) reset(); else dragging = true; lastClick = now; return true; }
        return false;
    }
    @Override public boolean mouseDragged(double x, double y, int b, double dx, double dy) {
        if (dragging && texture != null && b == 0) { panX += dx; panY += dy; return true; }
        return super.mouseDragged(x, y, b, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int b) { dragging = false; return super.mouseReleased(x, y, b); }
    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (texture != null && amount != 0 && area().contains(x, y)) { zoom = Math.max(.1, Math.min(12, zoom * Math.pow(1.2, amount))); return true; }
        return super.mouseScrolled(x, y, amount);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (texture != null && keyCode == GLFW.GLFW_KEY_R) { reset(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public void removed() { generation++; destroyTexture(); super.removed(); }
    @Override public boolean isPauseScreen() { return false; }

    private void load(boolean refresh) {
        final int request = ++generation; error = ""; reset(); destroyTexture();
        RemoteImageLoader.INSTANCE.loadImage(uri, refresh).whenComplete((decoded, throwable) ->
                client.execute(() -> finish(request, decoded, throwable)));
    }
    private void finish(int request, DecodedImage decoded, Throwable throwable) {
        if (request != generation) return;
        if (throwable != null || decoded == null) error = errorText(throwable); else texture = new AnimatedFabricTexture(client, decoded);
    }
    private void copyUrl() { client.keyboard.setClipboard(uri.toASCIIString()); copiedUntil = System.nanoTime() + 2_000_000_000L; }
    private void destroyTexture() { if (texture != null) texture.close(); texture = null; }
    private void reset() { zoom = 1; panX = panY = 0; }
    private Area area() { return new Area(8, 25, Math.max(9, width - 8), Math.max(26, height - 51)); }
    private static String tr(String key, Object... args) { return I18n.translate(key, args); }
    private static String dots() { int n = (int) (System.currentTimeMillis() / 350 % 4); return n == 0 ? "" : n == 1 ? "." : n == 2 ? ".." : "..."; }
    private static String errorText(Throwable throwable) {
        Throwable cause = throwable; while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) cause = cause.getCause();
        if (cause instanceof ImageLoadException) { ImageLoadException e = (ImageLoadException) cause; return tr(e.translationKey(), e.arguments()); }
        return tr("picturebridge.error.network", cause == null || cause.getMessage() == null ? "unknown error" : cause.getMessage());
    }
    private static void border(MatrixStack m, Area a, int c) {
        fill(m, a.left, a.top, a.right, a.top + 1, c); fill(m, a.left, a.bottom - 1, a.right, a.bottom, c);
        fill(m, a.left, a.top, a.left + 1, a.bottom, c); fill(m, a.right - 1, a.top, a.right, a.bottom, c);
    }
    private static final class Area {
        final int left, top, right, bottom; Area(int l, int t, int r, int b) { left=l; top=t; right=r; bottom=b; }
        int width(){return right-left;} int height(){return bottom-top;} int cx(){return left+width()/2;} int cy(){return top+height()/2;}
        boolean contains(double x,double y){return x>=left&&x<right&&y>=top&&y<bottom;}
    }
}
