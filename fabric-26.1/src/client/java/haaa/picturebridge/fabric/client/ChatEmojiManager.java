package haaa.picturebridge.fabric.client;

import haaa.picturebridge.fabric.PictureBridgeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionException;

/** Downloads, caches and renders image/expression preview blocks inside chat. */
public final class ChatEmojiManager {
    public static final ChatEmojiManager INSTANCE = new ChatEmojiManager();

    private static final int MAX_TEXTURES = 32;
    private static final int MAX_INLINE_TEXTURE_SIDE = 64;
    private static final long RETRY_DELAY_NANOS = 60_000_000_000L;

    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>(32, 0.75F, true);

    private ChatEmojiManager() {
    }

    /** Draws every ShitBot expression run in an already wrapped chat line. */
    public void renderLine(GuiGraphicsExtractor graphics,
                           Font font,
                           FormattedCharSequence line,
                           int y,
                           float opacity) {
        List<Span> spans = new ArrayList<>();
        int[] cursorX = {0};
        int[] spanStart = {0};
        int[] activePreviewWidth = {0};
        URI[] activeUri = {null};

        line.accept((index, style, codePoint) -> {
            int previewWidth = ExpressionText.previewWidth(style);
            URI uri = previewWidth > 0
                    ? haaa.picturebridge.fabric.ShitBotImageLink.findInlinePreview(style)
                    : null;
            if (!Objects.equals(uri, activeUri[0]) || previewWidth != activePreviewWidth[0]) {
                if (activeUri[0] != null) {
                    spans.add(new Span(activeUri[0], spanStart[0], activePreviewWidth[0]));
                }
                activeUri[0] = uri;
                activePreviewWidth[0] = previewWidth;
                spanStart[0] = cursorX[0];
            }
            cursorX[0] += font.width(FormattedCharSequence.codepoint(codePoint, style));
            return true;
        });
        if (activeUri[0] != null) {
            spans.add(new Span(activeUri[0], spanStart[0], activePreviewWidth[0]));
        }

        for (Span span : spans) {
            double spacing = Minecraft.getInstance().options.chatLineSpacing().get();
            int lineHeight = (int) (9 * (spacing + 1.0));
            int blockTop = y - (ExpressionText.BLOCK_LINES - 1) * lineHeight;
            int blockHeight = (ExpressionText.BLOCK_LINES - 1) * lineHeight + 8;
            render(graphics,
                    span.uri(),
                    span.x(),
                    blockTop,
                    span.previewWidth(),
                    blockHeight,
                    opacity);
        }
    }

    public void render(GuiGraphicsExtractor graphics,
                       URI uri,
                       int slotX,
                       int slotY,
                       int slotWidth,
                       int slotHeight,
                       float opacity) {
        if (uri == null || opacity <= 1.0E-5F) {
            return;
        }

        String key = uri.toASCIIString();
        Entry entry = entries.get(key);
        long now = System.nanoTime();
        if (entry == null) {
            entry = new Entry(key);
            entries.put(key, entry);
            startLoad(entry, uri);
            evictIfNeeded();
            return;
        }
        if (entry.texture == null) {
            if (!entry.loading && now >= entry.retryAtNanos) {
                startLoad(entry, uri);
            }
            return;
        }

        entry.texture.update(now);
        int sourceWidth = entry.texture.width();
        int sourceHeight = entry.texture.height();
        double scale = Math.min(slotWidth / (double) sourceWidth, slotHeight / (double) sourceHeight);
        int drawnWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int drawnHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        int x = slotX + (slotWidth - drawnWidth) / 2;
        int y = slotY + (slotHeight - drawnHeight) / 2;
        int alpha = Math.max(0, Math.min(255, Math.round(opacity * 255.0F)));
        int color = alpha << 24 | 0xFFFFFF;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                entry.texture.id(),
                x,
                y,
                0.0F,
                0.0F,
                drawnWidth,
                drawnHeight,
                sourceWidth,
                sourceHeight,
                sourceWidth,
                sourceHeight,
                color
        );
    }

    private void startLoad(Entry entry, URI uri) {
        entry.loading = true;
        entry.retryAtNanos = Long.MAX_VALUE;
        RemoteImageLoader.INSTANCE.loadImage(uri, false).whenComplete((decoded, throwable) -> {
            Minecraft.getInstance().execute(() -> finishLoad(entry, uri, decoded, throwable));
        });
    }

    private void finishLoad(Entry entry,
                            URI uri,
                            RemoteImageLoader.DecodedImage decoded,
                            Throwable throwable) {
        Entry current = entries.get(entry.key);
        if (current != entry) {
            if (decoded != null) {
                decoded.close();
            }
            return;
        }

        entry.loading = false;
        if (throwable != null || decoded == null) {
            entry.retryAtNanos = System.nanoTime() + RETRY_DELAY_NANOS;
            PictureBridgeClient.LOGGER.debug("Failed to load inline QQ expression {}", uri, unwrap(throwable));
            return;
        }

        try {
            decoded = decoded.scaledToFit(MAX_INLINE_TEXTURE_SIDE);
            Identifier id = Identifier.fromNamespaceAndPath(
                    PictureBridgeClient.MOD_ID, "chat/" + urlHash(uri));
            entry.texture = AnimatedTexture.register(Minecraft.getInstance(), id, decoded);
            entry.retryAtNanos = Long.MAX_VALUE;
        } catch (RuntimeException exception) {
            decoded.close();
            entry.retryAtNanos = System.nanoTime() + RETRY_DELAY_NANOS;
            PictureBridgeClient.LOGGER.warn("Failed to create inline QQ expression texture {}", uri, exception);
        }
    }

    private void evictIfNeeded() {
        Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
        while (entries.size() > MAX_TEXTURES && iterator.hasNext()) {
            Entry eldest = iterator.next().getValue();
            iterator.remove();
            if (eldest.texture != null) {
                eldest.texture.close();
                eldest.texture = null;
            }
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
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

    private static final class Entry {
        private final String key;
        private AnimatedTexture texture;
        private boolean loading;
        private long retryAtNanos;

        private Entry(String key) {
            this.key = key;
        }
    }

    private record Span(URI uri, int x, int previewWidth) {
    }
}
