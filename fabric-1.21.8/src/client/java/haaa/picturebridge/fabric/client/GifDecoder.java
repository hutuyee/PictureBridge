package haaa.picturebridge.fabric.client;

import net.minecraft.client.texture.NativeImage;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Decodes and composites every frame of a GIF using the JDK's built-in GIF reader. */
final class GifDecoder {
    private static final String STREAM_METADATA = "javax_imageio_gif_stream_1.0";
    private static final String IMAGE_METADATA = "javax_imageio_gif_image_1.0";
    private static final long MAX_ANIMATION_PIXELS =
            (long) RemoteImageLoader.MAX_ANIMATION_MEGAPIXELS * 1_000_000L;

    private GifDecoder() {
    }

    static RemoteImageLoader.DecodedImage decode(byte[] bytes) throws ImageLoadException {
        List<RemoteImageLoader.Frame> decodedFrames = new ArrayList<>();
        ImageReader reader = null;
        try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
            if (!readers.hasNext()) {
                throw new ImageLoadException("picturebridge.error.decode");
            }

            reader = readers.next();
            reader.setInput(input, false, false);
            int frameCount = reader.getNumImages(true);
            if (frameCount <= 0 || frameCount > RemoteImageLoader.MAX_ANIMATION_FRAMES) {
                throw new ImageLoadException(
                        "picturebridge.error.animation_too_large",
                        RemoteImageLoader.MAX_ANIMATION_FRAMES,
                        RemoteImageLoader.MAX_ANIMATION_MEGAPIXELS);
            }

            ImageHeaderProbe.Dimensions header = ImageHeaderProbe.inspect(bytes);
            int canvasWidth = header == null ? reader.getWidth(0) : header.width();
            int canvasHeight = header == null ? reader.getHeight(0) : header.height();
            int[] logicalSize = readLogicalSize(reader.getStreamMetadata());
            if (logicalSize != null) {
                canvasWidth = logicalSize[0];
                canvasHeight = logicalSize[1];
            }
            RemoteImageLoader.validateDimensions(canvasWidth, canvasHeight);
            if ((long) canvasWidth * canvasHeight * frameCount > MAX_ANIMATION_PIXELS) {
                throw new ImageLoadException(
                        "picturebridge.error.animation_too_large",
                        RemoteImageLoader.MAX_ANIMATION_FRAMES,
                        RemoteImageLoader.MAX_ANIMATION_MEGAPIXELS);
            }

            BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB);
            FrameInfo previous = null;
            BufferedImage previousRestore = null;

            for (int index = 0; index < frameCount; index++) {
                if (previous != null) {
                    applyDisposal(canvas, previous, previousRestore);
                }

                FrameInfo info = readFrameInfo(reader.getImageMetadata(index));
                BufferedImage restore = info.restoreToPrevious() ? copy(canvas) : null;
                BufferedImage rawFrame = reader.read(index);
                RemoteImageLoader.validateDimensions(rawFrame.getWidth(), rawFrame.getHeight());

                Graphics2D graphics = canvas.createGraphics();
                try {
                    graphics.setComposite(AlphaComposite.SrcOver);
                    graphics.drawImage(rawFrame, info.left(), info.top(), null);
                } finally {
                    graphics.dispose();
                }

                decodedFrames.add(new RemoteImageLoader.Frame(
                        toNativeImage(canvas), normalizeDelay(info.delayHundredths())));
                previous = info;
                previousRestore = restore;
            }

            return new RemoteImageLoader.DecodedImage(decodedFrames, canvasWidth, canvasHeight);
        } catch (ImageLoadException exception) {
            closeFrames(decodedFrames);
            throw exception;
        } catch (IOException | RuntimeException exception) {
            closeFrames(decodedFrames);
            throw new ImageLoadException("picturebridge.error.decode", exception);
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    private static void applyDisposal(BufferedImage canvas, FrameInfo previous, BufferedImage restore) {
        if (previous.restoreToPrevious() && restore != null) {
            Graphics2D graphics = canvas.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Src);
                graphics.drawImage(restore, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        } else if (previous.restoreToBackground()) {
            Graphics2D graphics = canvas.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(previous.left(), previous.top(), previous.width(), previous.height());
            } finally {
                graphics.dispose();
            }
        }
    }

    private static NativeImage toNativeImage(BufferedImage source) {
        NativeImage image = new NativeImage(source.getWidth(), source.getHeight(), true);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                image.setColorArgb(x, y, source.getRGB(x, y));
            }
        }
        return image;
    }

    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static int normalizeDelay(int hundredths) {
        // Use a readable fallback only when an old GIF omits its frame delay.
        return hundredths <= 0 ? 100 : Math.min(10_000, hundredths * 10);
    }

    private static int[] readLogicalSize(IIOMetadata metadata) {
        Node descriptor = findNode(metadata, STREAM_METADATA, "LogicalScreenDescriptor");
        if (descriptor == null) {
            return null;
        }
        int width = attribute(descriptor, "logicalScreenWidth", -1);
        int height = attribute(descriptor, "logicalScreenHeight", -1);
        return width > 0 && height > 0 ? new int[]{width, height} : null;
    }

    private static FrameInfo readFrameInfo(IIOMetadata metadata) {
        Node descriptor = findNode(metadata, IMAGE_METADATA, "ImageDescriptor");
        Node control = findNode(metadata, IMAGE_METADATA, "GraphicControlExtension");
        int left = attribute(descriptor, "imageLeftPosition", 0);
        int top = attribute(descriptor, "imageTopPosition", 0);
        int width = attribute(descriptor, "imageWidth", 0);
        int height = attribute(descriptor, "imageHeight", 0);
        int delay = attribute(control, "delayTime", 10);
        String disposal = stringAttribute(control, "disposalMethod", "none");
        return new FrameInfo(left, top, width, height, delay,
                "restoreToBackgroundColor".equals(disposal),
                "restoreToPrevious".equals(disposal));
    }

    private static Node findNode(IIOMetadata metadata, String format, String name) {
        if (metadata == null) {
            return null;
        }
        try {
            return findNode(metadata.getAsTree(format), name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Node findNode(Node node, String name) {
        if (node == null) {
            return null;
        }
        if (name.equals(node.getNodeName())) {
            return node;
        }
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            Node found = findNode(child, name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static int attribute(Node node, String name, int fallback) {
        String value = stringAttribute(node, name, null);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String stringAttribute(Node node, String name, String fallback) {
        if (node == null) {
            return fallback;
        }
        NamedNodeMap attributes = node.getAttributes();
        Node attribute = attributes == null ? null : attributes.getNamedItem(name);
        return attribute == null ? fallback : attribute.getNodeValue();
    }

    private static void closeFrames(List<RemoteImageLoader.Frame> frames) {
        for (RemoteImageLoader.Frame frame : frames) {
            frame.image().close();
        }
        frames.clear();
    }

    private record FrameInfo(int left,
                             int top,
                             int width,
                             int height,
                             int delayHundredths,
                             boolean restoreToBackground,
                             boolean restoreToPrevious) {
    }
}
