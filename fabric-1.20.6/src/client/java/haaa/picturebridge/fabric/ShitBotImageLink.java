package haaa.picturebridge.fabric;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;

import java.net.URI;
import java.util.Locale;

/** Recognizes the rich chat components emitted for ShitBot QQ images and expressions. */
public final class ShitBotImageLink {
    private ShitBotImageLink() {
    }

    public static URI find(Style style) {
        URI uri = findHttpUrl(style);
        if (uri == null) {
            return null;
        }

        if (hasShitBotImageMarker(style) || hasShitBotExpressionMarker(style)) {
            return uri;
        }
        return null;
    }

    /** Returns only links explicitly marked by ShitBot as QQ expressions. */
    public static URI findExpression(Style style) {
        URI uri = findHttpUrl(style);
        return uri != null && hasShitBotExpressionMarker(style) ? uri : null;
    }

    /** Returns links that ShitBot explicitly marked for an inline image preview. */
    public static URI findInlinePreview(Style style) {
        URI uri = findHttpUrl(style);
        return uri != null && (hasShitBotImageMarker(style) || hasShitBotExpressionMarker(style))
                ? uri
                : null;
    }

    private static URI findHttpUrl(Style style) {
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
        return isHttp(uri) ? uri : null;
    }

    private static boolean hasShitBotImageMarker(Style style) {
        if (style.getHoverEvent() == null) {
            return false;
        }
        net.minecraft.text.Text hoverText = style.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverText == null) {
            return false;
        }
        String hover = hoverText.getString().toLowerCase(Locale.ROOT);
        return hover.contains("qq 图片") || hover.contains("qq image");
    }

    private static boolean hasShitBotExpressionMarker(Style style) {
        if (style.getHoverEvent() == null) {
            return false;
        }
        net.minecraft.text.Text hoverText = style.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT);
        if (hoverText == null) {
            return false;
        }
        String hover = hoverText.getString().toLowerCase(Locale.ROOT);
        return hover.contains("qq 表情") || hover.contains("qq expression") || hover.contains("qq emoji");
    }

    private static boolean isHttp(URI uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }
        return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    }
}
