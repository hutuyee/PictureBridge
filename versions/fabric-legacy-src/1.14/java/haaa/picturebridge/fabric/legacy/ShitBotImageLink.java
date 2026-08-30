package haaa.picturebridge.fabric.legacy;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.net.URI;
import java.util.Locale;

public final class ShitBotImageLink {
    private ShitBotImageLink() {
    }

    public static URI find(Style style) {
        ClickEvent click = style == null ? null : style.getClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.OPEN_URL) return null;

        URI uri;
        try {
            uri = URI.create(click.getValue());
        } catch (IllegalArgumentException exception) {
            return null;
        }
        if (uri.getScheme() == null || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))) return null;

        HoverEvent hover = style.getHoverEvent();
        Text markerText = hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT
                ? hover.getValue() : null;
        if (markerText == null) return null;
        String marker = markerText.asString().toLowerCase(Locale.ROOT);
        return marker.contains("qq 图片") || marker.contains("qq image")
                || marker.contains("qq 表情") || marker.contains("qq expression")
                || marker.contains("qq emoji") ? uri : null;
    }
}
