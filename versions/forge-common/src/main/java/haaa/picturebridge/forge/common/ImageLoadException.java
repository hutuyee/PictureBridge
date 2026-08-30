package haaa.picturebridge.forge.common;

import java.io.IOException;

public final class ImageLoadException extends IOException {
    private final String translationKey;
    private final Object[] arguments;

    public ImageLoadException(String translationKey, Object... arguments) {
        super(translationKey);
        this.translationKey = translationKey;
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    public ImageLoadException(String translationKey, Throwable cause, Object... arguments) {
        super(translationKey, cause);
        this.translationKey = translationKey;
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    public String translationKey() {
        return translationKey;
    }

    public Object[] arguments() {
        return arguments.clone();
    }
}
