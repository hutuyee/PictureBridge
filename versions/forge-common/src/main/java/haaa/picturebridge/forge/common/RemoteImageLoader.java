package haaa.picturebridge.forge.common;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.IDN;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Loader shared by the Java 8-era Forge adapters. */
public final class RemoteImageLoader {
    public static final RemoteImageLoader INSTANCE = new RemoteImageLoader();
    public static final int MAX_DOWNLOAD_MIB = 24;
    public static final int MAX_DIMENSION = 8192;
    public static final int MAX_MEGAPIXELS = 32;
    static final int MAX_ANIMATION_FRAMES = 256;
    static final int MAX_ANIMATION_MEGAPIXELS = 32;

    private static final int MAX_DOWNLOAD_BYTES = MAX_DOWNLOAD_MIB * 1024 * 1024;
    private static final long MAX_PIXELS = MAX_MEGAPIXELS * 1_000_000L;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_CACHE_ENTRIES = 8;
    private static final long MAX_CACHE_BYTES = 64L * 1024L * 1024L;

    private final ExecutorService executor = Executors.newFixedThreadPool(2, new LoaderThreadFactory());
    private final ConcurrentHashMap<String, CompletableFuture<DownloadedImage>> inFlight =
            new ConcurrentHashMap<String, CompletableFuture<DownloadedImage>>();
    private final Object cacheLock = new Object();
    private final LinkedHashMap<String, DownloadedImage> cache =
            new LinkedHashMap<String, DownloadedImage>(16, 0.75F, true);
    private long cachedBytes;

    private RemoteImageLoader() {
    }

    public CompletableFuture<DecodedImage> loadImage(final URI uri, boolean forceRefresh) {
        return loadBytes(uri, forceRefresh).thenApplyAsync(downloaded -> decode(downloaded.bytes), executor);
    }

    private CompletableFuture<DownloadedImage> loadBytes(final URI uri, boolean forceRefresh) {
        final String key = uri == null ? "" : uri.toASCIIString();
        if (!forceRefresh) {
            synchronized (cacheLock) {
                DownloadedImage cached = cache.get(key);
                if (cached != null) return CompletableFuture.completedFuture(cached);
            }
            CompletableFuture<DownloadedImage> active = inFlight.get(key);
            if (active != null) return active;
        } else {
            removeCached(key);
        }

        CompletableFuture<DownloadedImage> created = CompletableFuture.supplyAsync(() -> {
            try {
                return download(uri);
            } catch (ImageLoadException exception) {
                throw new CompletionException(exception);
            } catch (IOException exception) {
                throw new CompletionException(new ImageLoadException(
                        "picturebridge.error.network", exception, readableMessage(exception)));
            }
        }, executor);

        if (!forceRefresh) {
            CompletableFuture<DownloadedImage> existing = inFlight.putIfAbsent(key, created);
            if (existing != null) return existing;
        }

        created.whenComplete((downloaded, throwable) -> {
            if (!forceRefresh) inFlight.remove(key, created);
            if (downloaded != null) putCached(key, downloaded);
        });
        return created;
    }

    private DownloadedImage download(URI originalUri) throws IOException {
        URI current = validateUri(originalUri, "picturebridge.error.invalid_url");
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            rejectPrivateAddress(current);
            URLConnection rawConnection = current.toURL().openConnection();
            if (!(rawConnection instanceof HttpURLConnection)) {
                throw new ImageLoadException("picturebridge.error.invalid_url");
            }

            HttpURLConnection connection = (HttpURLConnection) rawConnection;
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "image/webp,image/png,image/jpeg,image/gif,image/*;q=0.8");
            connection.setRequestProperty("User-Agent", "PictureBridge/0.4 (Minecraft Forge)");
            int status;
            try {
                status = connection.getResponseCode();
                if (isRedirect(status)) {
                    if (redirect == MAX_REDIRECTS) {
                        throw new ImageLoadException("picturebridge.error.redirect");
                    }
                    String location = connection.getHeaderField("Location");
                    if (location == null || location.trim().isEmpty()) {
                        throw new ImageLoadException("picturebridge.error.redirect");
                    }
                    current = validateUri(current.resolve(location), "picturebridge.error.redirect");
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new ImageLoadException("picturebridge.error.http", status);
                }
                long contentLength = connection.getContentLengthLong();
                if (contentLength > MAX_DOWNLOAD_BYTES) {
                    throw new ImageLoadException("picturebridge.error.too_large", MAX_DOWNLOAD_MIB);
                }
                byte[] bytes;
                try (InputStream body = connection.getInputStream()) {
                    bytes = readLimited(body, contentLength);
                }
                ImageHeaderProbe.Dimensions dimensions = ImageHeaderProbe.inspect(bytes);
                if (dimensions == null) throw new ImageLoadException("picturebridge.error.unsupported");
                validateDimensions(dimensions.width, dimensions.height);
                return new DownloadedImage(bytes);
            } finally {
                connection.disconnect();
            }
        }
        throw new ImageLoadException("picturebridge.error.redirect");
    }

    private DecodedImage decode(byte[] bytes) {
        try {
            if (ImageHeaderProbe.isGif(bytes)) return GifDecoder.decode(bytes);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new ImageLoadException("picturebridge.error.decode");
            }
            validateDimensions(image.getWidth(), image.getHeight());
            BufferedImage argb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D graphics = argb.createGraphics();
            try {
                graphics.drawImage(image, 0, 0, null);
            } finally {
                graphics.dispose();
            }
            return DecodedImage.single(argb);
        } catch (ImageLoadException exception) {
            throw new CompletionException(exception);
        } catch (IOException exception) {
            throw new CompletionException(new ImageLoadException("picturebridge.error.decode", exception));
        }
    }

    private static URI validateUri(URI uri, String errorKey) throws ImageLoadException {
        if (uri == null || uri.isOpaque() || uri.getScheme() == null || uri.getHost() == null
                || uri.getRawUserInfo() != null) {
            throw new ImageLoadException(errorKey);
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) throw new ImageLoadException(errorKey);
        try {
            if (uri.getHost().indexOf(':') < 0) IDN.toASCII(uri.getHost(), IDN.USE_STD3_ASCII_RULES);
            String value = uri.toASCIIString();
            int fragment = value.indexOf('#');
            return URI.create(fragment < 0 ? value : value.substring(0, fragment)).normalize();
        } catch (IllegalArgumentException exception) {
            throw new ImageLoadException(errorKey, exception);
        }
    }

    private static void rejectPrivateAddress(URI uri) throws IOException {
        String host = uri.getHost().indexOf(':') >= 0 ? uri.getHost() : IDN.toASCII(uri.getHost());
        InetAddress[] addresses = InetAddress.getAllByName(host);
        if (addresses.length == 0) {
            throw new ImageLoadException("picturebridge.error.network", "DNS returned no address");
        }
        for (InetAddress address : addresses) {
            if (!isPublicAddress(address)) throw new ImageLoadException("picturebridge.error.blocked_address");
        }
    }

    private static boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] raw = address.getAddress();
        if (raw.length == 4) {
            int first = raw[0] & 0xFF, second = raw[1] & 0xFF;
            return first != 0 && first != 127 && first != 255
                    && !(first == 100 && second >= 64 && second <= 127);
        }
        if (raw.length == 16) {
            int first = raw[0] & 0xFF, second = raw[1] & 0xFF;
            return (first & 0xFE) != 0xFC && !(first == 0xFE && (second & 0xC0) == 0x80);
        }
        return false;
    }

    private static byte[] readLimited(InputStream input, long declaredLength) throws IOException {
        int initial = declaredLength > 0 ? (int) Math.min(declaredLength, MAX_DOWNLOAD_BYTES) : 16384;
        ByteArrayOutputStream output = new ByteArrayOutputStream(initial);
        byte[] buffer = new byte[16384];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            if (read == 0) continue;
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
            throw new ImageLoadException("picturebridge.error.dimensions", MAX_DIMENSION, MAX_MEGAPIXELS);
        }
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    private void putCached(String key, DownloadedImage downloaded) {
        synchronized (cacheLock) {
            DownloadedImage previous = cache.put(key, downloaded);
            if (previous != null) cachedBytes -= previous.bytes.length;
            cachedBytes += downloaded.bytes.length;
            Iterator<Map.Entry<String, DownloadedImage>> iterator = cache.entrySet().iterator();
            while ((cache.size() > MAX_CACHE_ENTRIES || cachedBytes > MAX_CACHE_BYTES) && iterator.hasNext()) {
                Map.Entry<String, DownloadedImage> eldest = iterator.next();
                cachedBytes -= eldest.getValue().bytes.length;
                iterator.remove();
            }
        }
    }

    private void removeCached(String key) {
        synchronized (cacheLock) {
            DownloadedImage removed = cache.remove(key);
            if (removed != null) cachedBytes -= removed.bytes.length;
        }
    }

    private static String readableMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.trim().isEmpty() ? throwable.getClass().getSimpleName() : message;
    }

    private static final class DownloadedImage {
        private final byte[] bytes;
        private DownloadedImage(byte[] bytes) { this.bytes = bytes; }
    }

    private static final class LoaderThreadFactory implements ThreadFactory {
        private final AtomicInteger sequence = new AtomicInteger();
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "picturebridge-forge-image-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
