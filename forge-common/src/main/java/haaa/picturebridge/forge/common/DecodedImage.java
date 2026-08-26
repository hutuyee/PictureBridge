package haaa.picturebridge.forge.common;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DecodedImage {
    private final List<Frame> frames;
    private final int width;
    private final int height;

    public DecodedImage(List<Frame> frames, int width, int height) {
        if (frames == null || frames.isEmpty()) {
            throw new IllegalArgumentException("An image must contain at least one frame");
        }
        this.frames = Collections.unmodifiableList(new ArrayList<Frame>(frames));
        this.width = width;
        this.height = height;
    }

    public static DecodedImage single(BufferedImage image) {
        return new DecodedImage(
                Collections.singletonList(new Frame(image, Integer.MAX_VALUE)),
                image.getWidth(),
                image.getHeight());
    }

    public List<Frame> frames() {
        return frames;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean animated() {
        return frames.size() > 1;
    }

    public DecodedImage scaledToFit(int maximumSide) {
        if (width <= maximumSide && height <= maximumSide) {
            return this;
        }

        double scale = Math.min(maximumSide / (double) width, maximumSide / (double) height);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        List<Frame> scaled = new ArrayList<Frame>(frames.size());
        for (Frame frame : frames) {
            BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = target.createGraphics();
            try {
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                graphics.drawImage(frame.image(), 0, 0, targetWidth, targetHeight, null);
            } finally {
                graphics.dispose();
            }
            scaled.add(new Frame(target, frame.durationMillis()));
        }
        return new DecodedImage(scaled, targetWidth, targetHeight);
    }

    public static final class Frame {
        private final BufferedImage image;
        private final int durationMillis;

        public Frame(BufferedImage image, int durationMillis) {
            if (image == null) {
                throw new IllegalArgumentException("Frame image cannot be null");
            }
            this.image = image;
            this.durationMillis = Math.max(20, durationMillis);
        }

        public BufferedImage image() {
            return image;
        }

        public int durationMillis() {
            return durationMillis;
        }
    }
}
