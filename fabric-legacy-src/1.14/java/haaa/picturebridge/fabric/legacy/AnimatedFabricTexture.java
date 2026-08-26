package haaa.picturebridge.fabric.legacy;

import haaa.picturebridge.forge.common.DecodedImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;

final class AnimatedFabricTexture implements AutoCloseable {
    private final MinecraftClient client;
    private final DecodedImage decoded;
    private final NativeImageBackedTexture texture;
    private final Identifier identifier;
    private int frameIndex;
    private long nextFrameAtNanos;

    AnimatedFabricTexture(MinecraftClient client, DecodedImage decoded) {
        this.client = client;
        this.decoded = decoded;
        NativeImage pixels = new NativeImage(decoded.width(), decoded.height(), true);
        this.texture = new NativeImageBackedTexture(pixels);
        upload(decoded.frames().get(0).image());
        this.identifier = client.getTextureManager().registerDynamicTexture("picturebridge", texture);
        this.nextFrameAtNanos = System.nanoTime()
                + decoded.frames().get(0).durationMillis() * 1_000_000L;
    }

    Identifier identifier() { return identifier; }
    int width() { return decoded.width(); }
    int height() { return decoded.height(); }
    int frameCount() { return decoded.frames().size(); }
    boolean animated() { return decoded.animated(); }

    void update(long nowNanos) {
        if (!decoded.animated() || nowNanos < nextFrameAtNanos) return;
        int advances = 0;
        do {
            frameIndex = (frameIndex + 1) % decoded.frames().size();
            nextFrameAtNanos += decoded.frames().get(frameIndex).durationMillis() * 1_000_000L;
            advances++;
        } while (nowNanos >= nextFrameAtNanos && advances < decoded.frames().size());
        upload(decoded.frames().get(frameIndex).image());
    }

    private void upload(BufferedImage frame) {
        NativeImage pixels = texture.getImage();
        if (pixels == null) return;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                pixels.setPixelRgba(x, y, argbToAbgr(frame.getRGB(x, y)));
            }
        }
        texture.upload();
    }

    private static int argbToAbgr(int color) {
        return color & 0xFF00FF00 | color >> 16 & 0xFF | (color & 0xFF) << 16;
    }

    @Override
    public void close() {
        client.getTextureManager().destroyTexture(identifier);
    }
}
