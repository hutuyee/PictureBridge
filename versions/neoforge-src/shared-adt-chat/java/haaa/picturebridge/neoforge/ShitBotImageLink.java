package haaa.picturebridge.neoforge;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.util.Locale;

public final class ShitBotImageLink {
    private ShitBotImageLink() {
    }

    public static URI find(Style style) {
        if (style == null || !(style.getClickEvent() instanceof ClickEvent.OpenUrl openUrl)) {
            return null;
        }
        URI uri = openUrl.uri();
        if (!isHttp(uri) || !(style.getHoverEvent() instanceof HoverEvent.ShowText showText)) {
            return null;
        }
        String marker = showText.value().getString().toLowerCase(Locale.ROOT);
        return marker.contains("qq 图片") || marker.contains("qq image")
                || marker.contains("qq 表情") || marker.contains("qq expression")
                || marker.contains("qq emoji") ? uri : null;
    }

    private static boolean isHttp(URI uri) {
        return uri != null && uri.getScheme() != null
                && ("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()));
    }
}
