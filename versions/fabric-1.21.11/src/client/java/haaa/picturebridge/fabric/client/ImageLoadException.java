package haaa.picturebridge.fabric.client;

import java.io.IOException;

final class ImageLoadException extends IOException {
    private final String translationKey;
    private final Object[] arguments;

    ImageLoadException(String translationKey, Object... arguments) {
        super(translationKey);
        this.translationKey = translationKey;
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    ImageLoadException(String translationKey, Throwable cause, Object... arguments) {
        super(translationKey, cause);
        this.translationKey = translationKey;
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
    }

    String translationKey() {
        return translationKey;
    }

    Object[] arguments() {
        return arguments.clone();
    }
}
