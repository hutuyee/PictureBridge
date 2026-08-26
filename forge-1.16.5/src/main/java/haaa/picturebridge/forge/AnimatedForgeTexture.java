package haaa.picturebridge.forge;

import com.mojang.blaze3d.platform.NativeImage;
import haaa.picturebridge.forge.common.DecodedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;

final class AnimatedForgeTexture implements AutoCloseable {
    private final Minecraft minecraft;
    private final DecodedImage decoded;
    private final DynamicTexture texture;
    private final ResourceLocation location;
    private int frameIndex;
    private long nextFrameAtNanos;

    AnimatedForgeTexture(Minecraft minecraft, DecodedImage decoded) {
        this.minecraft = minecraft;
        this.decoded = decoded;
        this.texture = new DynamicTexture(decoded.width(), decoded.height(), true);
        upload(decoded.frames().get(0).image());
        this.location = minecraft.getTextureManager().register("picturebridge", texture);
        this.nextFrameAtNanos = System.nanoTime()
                + decoded.frames().get(0).durationMillis() * 1_000_000L;
    }

    ResourceLocation location() { return location; }
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

    private void upload(BufferedImage image) {
        NativeImage pixels = texture.getPixels();
        if (pixels == null) return;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                pixels.setPixelRGBA(x, y, argbToAbgr(image.getRGB(x, y)));
            }
        }
        texture.upload();
    }

    private static int argbToAbgr(int color) {
        return color & 0xFF00FF00 | color >> 16 & 0xFF | (color & 0xFF) << 16;
    }

    @Override
    public void close() {
        minecraft.getTextureManager().release(location);
    }
}
