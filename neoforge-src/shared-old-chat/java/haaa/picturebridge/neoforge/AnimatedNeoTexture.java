package haaa.picturebridge.neoforge;

import com.mojang.blaze3d.platform.NativeImage;
import haaa.picturebridge.forge.common.DecodedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

final class AnimatedNeoTexture implements AutoCloseable {
    private final Minecraft minecraft;
    private final DecodedImage decoded;
    private final List<NativeImage> frames = new ArrayList<>();
    private final DynamicTexture texture;
    private final ResourceLocation location;
    private int frameIndex;
    private long nextFrameAtNanos;

    AnimatedNeoTexture(Minecraft minecraft, DecodedImage decoded) {
        this.minecraft = minecraft;
        this.decoded = decoded;
        DynamicTexture created = null;
        NativeImage upload = null;
        try {
            for (DecodedImage.Frame frame : decoded.frames()) {
                frames.add(toNativeImage(frame));
            }
            upload = new NativeImage(decoded.width(), decoded.height(), true);
            upload.copyFrom(frames.get(0));
            created = new DynamicTexture(upload);
            this.texture = created;
            this.location = minecraft.getTextureManager().register("picturebridge", created);
            this.nextFrameAtNanos = System.nanoTime()
                    + decoded.frames().get(0).durationMillis() * 1_000_000L;
        } catch (RuntimeException | IOException exception) {
            if (created != null) {
                created.close();
            } else if (upload != null) {
                upload.close();
            }
            closeFrames();
            throw new IllegalStateException("Could not create PictureBridge texture", exception);
        }
    }

    ResourceLocation location() { return location; }
    int width() { return decoded.width(); }
    int height() { return decoded.height(); }
    int frameCount() { return decoded.frames().size(); }
    boolean animated() { return decoded.animated(); }

    void update(long nowNanos) {
        if (!decoded.animated() || nowNanos < nextFrameAtNanos) {
            return;
        }
        int advances = 0;
        do {
            frameIndex = (frameIndex + 1) % frames.size();
            nextFrameAtNanos += decoded.frames().get(frameIndex).durationMillis() * 1_000_000L;
            advances++;
        } while (nowNanos >= nextFrameAtNanos && advances < frames.size());
        NativeImage pixels = texture.getPixels();
        if (pixels != null) {
            pixels.copyFrom(frames.get(frameIndex));
            texture.upload();
        }
    }

    @Override
    public void close() {
        minecraft.getTextureManager().release(location);
        closeFrames();
    }

    private void closeFrames() {
        for (NativeImage frame : frames) {
            frame.close();
        }
        frames.clear();
    }

    private static NativeImage toNativeImage(DecodedImage.Frame frame) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(frame.image(), "png", output)) {
            throw new IOException("PNG encoder is unavailable");
        }
        return NativeImage.read(new ByteArrayInputStream(output.toByteArray()));
    }
}
