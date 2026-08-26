package haaa.picturebridge.forge;

import haaa.picturebridge.forge.common.DecodedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

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
        this.texture = new DynamicTexture(decoded.frames().get(0).image());
        this.location = minecraft.getTextureManager().getDynamicTextureLocation("picturebridge", texture);
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
        int[] pixels = texture.getTextureData();
        image.getRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        texture.updateDynamicTexture();
    }

    @Override
    public void close() {
        minecraft.getTextureManager().deleteTexture(location);
    }
}
