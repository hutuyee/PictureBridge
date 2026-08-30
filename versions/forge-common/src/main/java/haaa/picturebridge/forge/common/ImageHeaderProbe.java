package haaa.picturebridge.forge.common;

/** Reads dimensions before a decoder allocates pixel memory. */
final class ImageHeaderProbe {
    private ImageHeaderProbe() {
    }

    static Dimensions inspect(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        Dimensions dimensions = png(bytes);
        if (dimensions == null) dimensions = jpeg(bytes);
        if (dimensions == null) dimensions = gif(bytes);
        if (dimensions == null) dimensions = webp(bytes);
        if (dimensions == null) dimensions = bmp(bytes);
        return dimensions != null && dimensions.width > 0 && dimensions.height > 0 ? dimensions : null;
    }

    static boolean isGif(byte[] bytes) {
        return bytes != null && bytes.length >= 6
                && (ascii(bytes, 0, "GIF87a") || ascii(bytes, 0, "GIF89a"));
    }

    private static Dimensions png(byte[] bytes) {
        if (bytes.length < 24 || u8(bytes, 0) != 0x89 || bytes[1] != 'P' || bytes[2] != 'N'
                || bytes[3] != 'G' || u8(bytes, 4) != 0x0D || u8(bytes, 5) != 0x0A
                || u8(bytes, 6) != 0x1A || u8(bytes, 7) != 0x0A) {
            return null;
        }
        return new Dimensions(be32(bytes, 16), be32(bytes, 20));
    }

    private static Dimensions jpeg(byte[] bytes) {
        if (bytes.length < 4 || u8(bytes, 0) != 0xFF || u8(bytes, 1) != 0xD8) return null;
        int offset = 2;
        while (offset + 3 < bytes.length) {
            while (offset < bytes.length && u8(bytes, offset) != 0xFF) offset++;
            while (offset < bytes.length && u8(bytes, offset) == 0xFF) offset++;
            if (offset >= bytes.length) return null;
            int marker = u8(bytes, offset++);
            if (marker == 0xD9 || marker == 0xDA) return null;
            if (marker == 0x01 || marker >= 0xD0 && marker <= 0xD7) continue;
            if (offset + 1 >= bytes.length) return null;
            int length = be16(bytes, offset);
            if (length < 2 || offset + length > bytes.length) return null;
            if (isStartOfFrame(marker) && length >= 7) {
                return new Dimensions(be16(bytes, offset + 5), be16(bytes, offset + 3));
            }
            offset += length;
        }
        return null;
    }

    private static boolean isStartOfFrame(int marker) {
        return marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
    }

    private static Dimensions gif(byte[] bytes) {
        return bytes.length >= 10 && isGif(bytes) ? new Dimensions(le16(bytes, 6), le16(bytes, 8)) : null;
    }

    private static Dimensions webp(byte[] bytes) {
        if (bytes.length < 30 || !ascii(bytes, 0, "RIFF") || !ascii(bytes, 8, "WEBP")) return null;
        if (ascii(bytes, 12, "VP8X")) return new Dimensions(1 + le24(bytes, 24), 1 + le24(bytes, 27));
        if (ascii(bytes, 12, "VP8L") && bytes.length >= 25 && u8(bytes, 20) == 0x2F) {
            int b1 = u8(bytes, 21), b2 = u8(bytes, 22), b3 = u8(bytes, 23), b4 = u8(bytes, 24);
            return new Dimensions(1 + b1 + ((b2 & 0x3F) << 8),
                    1 + ((b2 & 0xC0) >> 6) + (b3 << 2) + ((b4 & 0x0F) << 10));
        }
        if (ascii(bytes, 12, "VP8 ") && bytes.length >= 30 && u8(bytes, 23) == 0x9D
                && u8(bytes, 24) == 0x01 && u8(bytes, 25) == 0x2A) {
            return new Dimensions(le16(bytes, 26) & 0x3FFF, le16(bytes, 28) & 0x3FFF);
        }
        return null;
    }

    private static Dimensions bmp(byte[] bytes) {
        if (bytes.length < 26 || bytes[0] != 'B' || bytes[1] != 'M') return null;
        int dibSize = le32(bytes, 14);
        if (dibSize == 12) return new Dimensions(le16(bytes, 18), le16(bytes, 20));
        if (dibSize >= 40) {
            int rawHeight = le32(bytes, 22);
            return new Dimensions(le32(bytes, 18), rawHeight == Integer.MIN_VALUE ? -1 : Math.abs(rawHeight));
        }
        return null;
    }

    private static boolean ascii(byte[] bytes, int offset, String value) {
        if (offset < 0 || offset + value.length() > bytes.length) return false;
        for (int i = 0; i < value.length(); i++) if (u8(bytes, offset + i) != value.charAt(i)) return false;
        return true;
    }

    private static int u8(byte[] bytes, int offset) { return bytes[offset] & 0xFF; }
    private static int be16(byte[] bytes, int offset) { return u8(bytes, offset) << 8 | u8(bytes, offset + 1); }
    private static int be32(byte[] bytes, int offset) {
        return u8(bytes, offset) << 24 | u8(bytes, offset + 1) << 16
                | u8(bytes, offset + 2) << 8 | u8(bytes, offset + 3);
    }
    private static int le16(byte[] bytes, int offset) { return u8(bytes, offset) | u8(bytes, offset + 1) << 8; }
    private static int le24(byte[] bytes, int offset) {
        return u8(bytes, offset) | u8(bytes, offset + 1) << 8 | u8(bytes, offset + 2) << 16;
    }
    private static int le32(byte[] bytes, int offset) {
        return u8(bytes, offset) | u8(bytes, offset + 1) << 8
                | u8(bytes, offset + 2) << 16 | u8(bytes, offset + 3) << 24;
    }

    static final class Dimensions {
        final int width;
        final int height;
        Dimensions(int width, int height) { this.width = width; this.height = height; }
    }
}
