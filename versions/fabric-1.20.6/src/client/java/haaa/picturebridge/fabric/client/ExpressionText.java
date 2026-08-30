package haaa.picturebridge.fabric.client;

import haaa.picturebridge.fabric.ShitBotImageLink;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.net.URI;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts ShitBot image and expression labels into styled multi-line preview slots. */
public final class ExpressionText {
    public static final int BLOCK_LINES = 6;
    public static final int EXPRESSION_PREVIEW_WIDTH = 48;
    public static final int IMAGE_PREVIEW_WIDTH = 96;

    private static final String EXPRESSION_ANCHOR = "picturebridge:inline-expression-anchor";
    private static final String IMAGE_ANCHOR = "picturebridge:inline-image-anchor";
    private static final String QQ_FACE_BASE = "https://koishi.js.org/QFace/gif/s";
    private static final Pattern LEGACY_FACE = Pattern.compile("\\[表情\\s*:?\\s*(\\d{1,5})]", Pattern.CASE_INSENSITIVE);
    private static final String EXPRESSION_ROW = "\u00A0".repeat(12);
    private static final String IMAGE_ROW = "\u00A0".repeat(24);

    private ExpressionText() {
    }

    public static Text replaceExpressions(Text original) {
        Builder builder = new Builder();
        URI[] activeStyledMedia = {null};

        original.visit((style, string) -> {
            URI expression = ShitBotImageLink.findExpression(style);
            URI media = expression != null ? expression : ShitBotImageLink.findInlinePreview(style);
            if (media != null) {
                if (!media.equals(activeStyledMedia[0])) {
                    builder.appendMedia(media, style, expression != null);
                }
                activeStyledMedia[0] = media;
            } else {
                activeStyledMedia[0] = null;
                builder.appendTextWithLegacyFaces(string, style);
            }
            return Optional.empty();
        }, Style.EMPTY);

        return builder.foundExpression ? builder.result : original;
    }

    public static boolean isAnchor(Style style) {
        return previewWidth(style) > 0;
    }

    public static int previewWidth(Style style) {
        if (style == null || ShitBotImageLink.findInlinePreview(style) == null) {
            return 0;
        }
        if (EXPRESSION_ANCHOR.equals(style.getInsertion())) {
            return EXPRESSION_PREVIEW_WIDTH;
        }
        if (IMAGE_ANCHOR.equals(style.getInsertion())) {
            return IMAGE_PREVIEW_WIDTH;
        }
        return 0;
    }

    private static URI legacyFaceUri(String id) {
        return URI.create(QQ_FACE_BASE + id + ".gif");
    }

    private static Style expressionStyle(Style original, URI uri) {
        return original
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, uri.toASCIIString()))
                .withHoverEvent(new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Text.literal("点击打开 QQ 表情")));
    }

    private static final class Builder {
        private final MutableText result = Text.empty();
        private boolean foundExpression;
        private boolean hasOutput;
        private boolean atLineStart = true;
        private boolean afterExpression;

        private void appendTextWithLegacyFaces(String value, Style style) {
            // A clickable label without the PictureBridge marker belongs to browser mode.
            if (style != null && style.getClickEvent() != null) {
                appendPlain(value, style);
                return;
            }
            Matcher matcher = LEGACY_FACE.matcher(value);
            int start = 0;
            while (matcher.find()) {
                appendPlain(value.substring(start, matcher.start()), style);
                URI uri = legacyFaceUri(matcher.group(1));
                appendMedia(uri, expressionStyle(style, uri), true);
                start = matcher.end();
            }
            appendPlain(value.substring(start), style);
        }

        private void appendMedia(URI uri, Style mediaStyle, boolean expression) {
            if (hasOutput && !atLineStart) {
                appendRaw("\n", Style.EMPTY);
            }

            String row = expression ? EXPRESSION_ROW : IMAGE_ROW;
            for (int line = 0; line < BLOCK_LINES - 1; line++) {
                appendRaw(row + "\n", mediaStyle);
            }
            appendRaw(row, mediaStyle.withInsertion(expression ? EXPRESSION_ANCHOR : IMAGE_ANCHOR));
            foundExpression = true;
            afterExpression = true;
        }

        private void appendPlain(String value, Style style) {
            if (value.isEmpty()) {
                return;
            }

            if (afterExpression) {
                int firstVisible = 0;
                while (firstVisible < value.length()) {
                    char character = value.charAt(firstVisible);
                    if (character != ' ' && character != '\t' && character != '\r') {
                        break;
                    }
                    firstVisible++;
                }
                value = value.substring(firstVisible);
                if (value.isEmpty()) {
                    return;
                }
                if (!atLineStart && value.charAt(0) != '\n') {
                    appendRaw("\n", Style.EMPTY);
                }
                afterExpression = false;
            }

            appendRaw(value, style);
        }

        private void appendRaw(String value, Style style) {
            if (value.isEmpty()) {
                return;
            }
            result.append(Text.literal(value).setStyle(style));
            hasOutput = true;
            atLineStart = value.charAt(value.length() - 1) == '\n';
        }
    }
}
