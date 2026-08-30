package haaa.picturebridge.fabric.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Owns a registered GPU texture and advances it through decoded animation frames. */
final class AnimatedTexture implements AutoCloseable {
    private final MinecraftClient client;
    private final Identifier id;
    private final RemoteImageLoader.DecodedImage decoded;
    private final NativeImageBackedTexture texture;
    private int frameIndex;
    private long nextFrameAtNanos;
    private boolean closed;

    private AnimatedTexture(MinecraftClient client,
                            Identifier id,
                            RemoteImageLoader.DecodedImage decoded) {
        this.client = client;
        this.id = id;
        this.decoded = decoded;

        NativeImage uploadImage = new NativeImage(decoded.width(), decoded.height(), true);
        NativeImageBackedTexture created = null;
        try {
            uploadImage.copyFrom(decoded.frames().getFirst().image());
            created = new NativeImageBackedTexture(
                    () -> "PictureBridge animated image " + id, uploadImage);
            client.getTextureManager().registerTexture(id, created);
            this.texture = created;
            this.nextFrameAtNanos = System.nanoTime()
                    + millisToNanos(decoded.frames().getFirst().durationMillis());
        } catch (RuntimeException exception) {
            if (created != null) {
                created.close();
            } else {
                uploadImage.close();
            }
            decoded.close();
            throw exception;
        }
    }

    static AnimatedTexture register(MinecraftClient client,
                                    Identifier id,
                                    RemoteImageLoader.DecodedImage decoded) {
        return new AnimatedTexture(client, id, decoded);
    }

    Identifier id() {
        return id;
    }

    int width() {
        return decoded.width();
    }

    int height() {
        return decoded.height();
    }

    int frameCount() {
        return decoded.frames().size();
    }

    boolean animated() {
        return decoded.animated();
    }

    void update(long nowNanos) {
        if (closed || !decoded.animated() || nowNanos < nextFrameAtNanos) {
            return;
        }

        int advances = 0;
        do {
            frameIndex = (frameIndex + 1) % decoded.frames().size();
            nextFrameAtNanos += millisToNanos(decoded.frames().get(frameIndex).durationMillis());
            advances++;
        } while (nowNanos >= nextFrameAtNanos && advances < decoded.frames().size());

        if (nowNanos >= nextFrameAtNanos) {
            nextFrameAtNanos = nowNanos
                    + millisToNanos(decoded.frames().get(frameIndex).durationMillis());
        }

        NativeImage uploadImage = texture.getImage();
        if (uploadImage != null) {
            uploadImage.copyFrom(decoded.frames().get(frameIndex).image());
            texture.upload();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            client.getTextureManager().destroyTexture(id);
        } finally {
            decoded.close();
        }
    }

    private static long millisToNanos(int milliseconds) {
        return milliseconds * 1_000_000L;
    }
}
