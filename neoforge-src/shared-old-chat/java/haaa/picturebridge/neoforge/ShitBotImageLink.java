package haaa.picturebridge.neoforge;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.net.URI;
import java.util.Locale;

public final class ShitBotImageLink {
    private ShitBotImageLink() {
    }

    public static URI find(Style style) {
        if (style == null || style.getClickEvent() == null
                || style.getClickEvent().getAction() != ClickEvent.Action.OPEN_URL) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(style.getClickEvent().getValue());
        } catch (IllegalArgumentException exception) {
            return null;
        }
        if (!isHttp(uri) || style.getHoverEvent() == null) {
            return null;
        }
        Component hover = style.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT);
        if (hover == null) {
            return null;
        }
        String marker = hover.getString().toLowerCase(Locale.ROOT);
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
