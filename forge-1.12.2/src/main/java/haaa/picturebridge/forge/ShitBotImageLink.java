package haaa.picturebridge.forge;

import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.ITextComponent;

import java.net.URI;
import java.util.Locale;

final class ShitBotImageLink {
    private ShitBotImageLink() {
    }

    static URI find(Style style) {
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
        if (!isHttp(uri)) return null;

        HoverEvent hoverEvent = style.getHoverEvent();
        ITextComponent hover = hoverEvent != null && hoverEvent.getAction() == HoverEvent.Action.SHOW_TEXT
                ? hoverEvent.getValue()
                : null;
        if (hover == null) return null;
        String marker = hover.getUnformattedText().toLowerCase(Locale.ROOT);
        return marker.contains("qq 图片") || marker.contains("qq image")
                || marker.contains("qq 表情") || marker.contains("qq expression")
                || marker.contains("qq emoji") ? uri : null;
    }

    private static boolean isHttp(URI uri) {
        return uri != null && uri.getScheme() != null
                && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
    }
}
