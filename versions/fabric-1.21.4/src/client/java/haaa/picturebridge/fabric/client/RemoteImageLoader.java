package haaa.picturebridge.fabric.client;

import haaa.picturebridge.fabric.PictureBridgeClient;
import net.minecraft.client.texture.NativeImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class RemoteImageLoader {
    static final RemoteImageLoader INSTANCE = new RemoteImageLoader();
    static final int MAX_DOWNLOAD_MIB = 24;
    static final int MAX_DIMENSION = 8192;
    static final int MAX_MEGAPIXELS = 32;
    static final int MAX_ANIMATION_FRAMES = 256;
    static final int MAX_ANIMATION_MEGAPIXELS = 32;

    private static final int MAX_DOWNLOAD_BYTES = MAX_DOWNLOAD_MIB * 1024 * 1024;
    private static final long MAX_PIXELS = MAX_MEGAPIXELS * 1_000_000L;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_CACHE_ENTRIES = 8;
    private static final long MAX_CACHE_BYTES = 64L * 1024L * 1024L;

    private final ExecutorService executor = Executors.newFixedThreadPool(2, new LoaderThreadFactory());
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final ConcurrentHashMap<String, CompletableFuture<DownloadedImage>> inFlight = new ConcurrentHashMap<>();
    private final Object cacheLock = new Object();
    private final LinkedHashMap<String, DownloadedImage> cache = new LinkedHashMap<>(16, 0.75F, true);
    private long cachedBytes;

    private RemoteImageLoader() {
    }

    CompletableFuture<DecodedImage> loadImage(URI uri, boolean forceRefresh) {
        return loadBytes(uri, forceRefresh).thenApplyAsync(this::decode, executor);
    }

    private CompletableFuture<DownloadedImage> loadBytes(URI uri, boolean forceRefresh) {
        String key = uri == null ? "" : uri.toASCIIString();
        if (!forceRefresh) {
            synchronized (cacheLock) {
                DownloadedImage cached = cache.get(key);
                if (cached != null) {
                    return CompletableFuture.completedFuture(cached);
                }
            }
            CompletableFuture<DownloadedImage> active = inFlight.get(key);
            if (active != null) {
                return active;
            }
        } else {
            removeCached(key);
        }

        CompletableFuture<DownloadedImage> created = CompletableFuture.supplyAsync(() -> {
            try {
                return download(uri);
            } catch (ImageLoadException exception) {
                throw new CompletionException(exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new CompletionException(new ImageLoadException(
                        "picturebridge.error.network", exception, "interrupted"));
            } catch (IOException exception) {
                throw new CompletionException(new ImageLoadException(
                        "picturebridge.error.network", exception, readableMessage(exception)));
            } catch (RuntimeException exception) {
                throw new CompletionException(new ImageLoadException(
                        "picturebridge.error.network", exception, readableMessage(exception)));
            }
        }, executor);

        if (!forceRefresh) {
            CompletableFuture<DownloadedImage> existing = inFlight.putIfAbsent(key, created);
            if (existing != null) {
                return existing;
            }
        }

        created.whenComplete((downloaded, throwable) -> {
            if (!forceRefresh) {
                inFlight.remove(key, created);
            }
            if (downloaded != null) {
                putCached(key, downloaded);
            }
        });
        return created;
    }

    private DownloadedImage download(URI originalUri) throws IOException, InterruptedException {
        URI current = validateUri(originalUri, "picturebridge.error.invalid_url");
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            rejectPrivateAddress(current);

            HttpRequest request = HttpRequest.newBuilder(current)
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "image/webp,image/png,image/jpeg,image/gif,image/*;q=0.8")
                    .header("User-Agent", "PictureBridge/0.1 (Minecraft Fabric 1.21.4)")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();

            if (isRedirect(status)) {
                try (InputStream ignored = response.body()) {
                    if (redirect == MAX_REDIRECTS) {
                        throw new ImageLoadException("picturebridge.error.redirect");
                    }
                    String location = response.headers().firstValue("Location")
                            .orElseThrow(() -> new ImageLoadException("picturebridge.error.redirect"));
                    current = validateUri(current.resolve(location), "picturebridge.error.redirect");
                    continue;
                }
            }

            if (status < 200 || status >= 300) {
                try (InputStream ignored = response.body()) {
                    throw new ImageLoadException("picturebridge.error.http", status);
                }
            }

            OptionalLong contentLength = response.headers().firstValueAsLong("Content-Length");
            if (contentLength.isPresent() && contentLength.getAsLong() > MAX_DOWNLOAD_BYTES) {
                try (InputStream ignored = response.body()) {
                    throw new ImageLoadException("picturebridge.error.too_large", MAX_DOWNLOAD_MIB);
                }
            }

            byte[] bytes;
            try (InputStream body = response.body()) {
                bytes = readLimited(body, contentLength.orElse(-1L));
            }
            ImageHeaderProbe.Dimensions dimensions = ImageHeaderProbe.inspect(bytes);
            if (dimensions == null) {
                throw new ImageLoadException("picturebridge.error.unsupported");
            }
            validateDimensions(dimensions.width(), dimensions.height());
            return new DownloadedImage(bytes, dimensions);
        }
        throw new ImageLoadException("picturebridge.error.redirect");
    }

    private DecodedImage decode(DownloadedImage downloaded) {
        NativeImage image = null;
        try {
            if (ImageHeaderProbe.isGif(downloaded.bytes())) {
                return GifDecoder.decode(downloaded.bytes());
            }
            image = NativeImage.read(downloaded.bytes());
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new ImageLoadException("picturebridge.error.decode");
            }
            validateDimensions(image.getWidth(), image.getHeight());
            return DecodedImage.single(image);
        } catch (ImageLoadException exception) {
            if (image != null) {
                image.close();
            }
            throw new CompletionException(exception);
        } catch (IOException | RuntimeException exception) {
            if (image != null) {
                image.close();
            }
            throw new CompletionException(new ImageLoadException("picturebridge.error.decode", exception));
        }
    }

    private static URI validateUri(URI uri, String errorKey) throws ImageLoadException {
        if (uri == null || uri.isOpaque() || uri.getScheme() == null || uri.getHost() == null
                || uri.getRawUserInfo() != null) {
            throw new ImageLoadException(errorKey);
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new ImageLoadException(errorKey);
        }
        try {
            if (!uri.getHost().contains(":")) {
                IDN.toASCII(uri.getHost(), IDN.USE_STD3_ASCII_RULES);
            }
            String value = uri.toASCIIString();
            int fragment = value.indexOf('#');
            return URI.create(fragment < 0 ? value : value.substring(0, fragment)).normalize();
        } catch (IllegalArgumentException exception) {
            throw new ImageLoadException(errorKey, exception);
        }
    }

    private static void rejectPrivateAddress(URI uri) throws IOException {
        String host = uri.getHost().contains(":") ? uri.getHost() : IDN.toASCII(uri.getHost());
        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (addresses.length == 0) {
            throw new ImageLoadException("picturebridge.error.network", "DNS returned no address");
        }
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) {
                throw new ImageLoadException("picturebridge.error.blocked_address");
            }
        }
    }

    private static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }

        byte[] raw = address.getAddress();
        if (raw.length == 4) {
            int first = raw[0] & 0xFF;
            int second = raw[1] & 0xFF;
            return first != 0
                    && first != 127
                    && first != 255
                    && !(first == 100 && second >= 64 && second <= 127);
        }
        if (raw.length == 16) {
            int first = raw[0] & 0xFF;
            int second = raw[1] & 0xFF;
            return (first & 0xFE) != 0xFC && !(first == 0xFE && (second & 0xC0) == 0x80);
        }
        return false;
    }

    private static byte[] readLimited(InputStream input, long declaredLength) throws IOException {
        int initialSize = declaredLength > 0L
                ? (int) Math.min(declaredLength, MAX_DOWNLOAD_BYTES)
                : 16 * 1024;
        ByteArrayOutputStream output = new ByteArrayOutputStream(initialSize);
        byte[] buffer = new byte[16 * 1024];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) {
                continue;
            }
            total += read;
            if (total > MAX_DOWNLOAD_BYTES) {
                throw new ImageLoadException("picturebridge.error.too_large", MAX_DOWNLOAD_MIB);
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    static void validateDimensions(int width, int height) throws ImageLoadException {
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                || (long) width * height > MAX_PIXELS) {
            throw new ImageLoadException(
                    "picturebridge.error.dimensions", MAX_DIMENSION, MAX_MEGAPIXELS);
        }
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private void putCached(String key, DownloadedImage downloaded) {
        synchronized (cacheLock) {
            DownloadedImage previous = cache.put(key, downloaded);
            if (previous != null) {
                cachedBytes -= previous.bytes().length;
            }
            cachedBytes += downloaded.bytes().length;

            Iterator<Map.Entry<String, DownloadedImage>> iterator = cache.entrySet().iterator();
            while ((cache.size() > MAX_CACHE_ENTRIES || cachedBytes > MAX_CACHE_BYTES) && iterator.hasNext()) {
                Map.Entry<String, DownloadedImage> eldest = iterator.next();
                cachedBytes -= eldest.getValue().bytes().length;
                iterator.remove();
            }
        }
    }

    private void removeCached(String key) {
        synchronized (cacheLock) {
            DownloadedImage removed = cache.remove(key);
            if (removed != null) {
                cachedBytes -= removed.bytes().length;
            }
        }
    }

    private static String readableMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    static final class DecodedImage implements AutoCloseable {
        private final List<Frame> frames;
        private final int width;
        private final int height;
        private boolean closed;

        DecodedImage(List<Frame> frames, int width, int height) {
            if (frames == null || frames.isEmpty()) {
                throw new IllegalArgumentException("An image must contain at least one frame");
            }
            this.frames = List.copyOf(frames);
            this.width = width;
            this.height = height;
        }

        static DecodedImage single(NativeImage image) {
            return new DecodedImage(List.of(new Frame(image, Integer.MAX_VALUE)), image.getWidth(), image.getHeight());
        }

        List<Frame> frames() {
            return frames;
        }

        int width() {
            return width;
        }

        int height() {
            return height;
        }

        boolean animated() {
            return frames.size() > 1;
        }

        DecodedImage scaledToFit(int maximumSide) {
            if (width <= maximumSide && height <= maximumSide) {
                return this;
            }

            double scale = Math.min(maximumSide / (double) width, maximumSide / (double) height);
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));
            List<Frame> scaledFrames = new java.util.ArrayList<>(frames.size());
            try {
                for (Frame frame : frames) {
                    NativeImage source = frame.image();
                    NativeImage target = new NativeImage(targetWidth, targetHeight, true);
                    scaledFrames.add(new Frame(target, frame.durationMillis()));
                    for (int y = 0; y < targetHeight; y++) {
                        int sourceY = Math.min(height - 1, y * height / targetHeight);
                        for (int x = 0; x < targetWidth; x++) {
                            int sourceX = Math.min(width - 1, x * width / targetWidth);
                            target.setColorArgb(x, y, source.getColorArgb(sourceX, sourceY));
                        }
                    }
                }
                DecodedImage scaled = new DecodedImage(scaledFrames, targetWidth, targetHeight);
                close();
                return scaled;
            } catch (RuntimeException exception) {
                for (Frame frame : scaledFrames) {
                    frame.image().close();
                }
                throw exception;
            }
        }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            for (Frame frame : frames) {
                frame.image().close();
            }
        }
    }

    record Frame(NativeImage image, int durationMillis) {
        Frame {
            if (image == null) {
                throw new IllegalArgumentException("Frame image cannot be null");
            }
            durationMillis = Math.max(20, durationMillis);
        }
    }

    private record DownloadedImage(byte[] bytes, ImageHeaderProbe.Dimensions dimensions) {
    }

    private static final class LoaderThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "picturebridge-image-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((ignored, throwable) ->
                    PictureBridgeClient.LOGGER.error("Uncaught image loading error", throwable));
            return thread;
        }
    }
}
