package haaa.picturebridge.fabric;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PictureBridgeClient implements ClientModInitializer {
    public static final String MOD_ID = "picturebridge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("PictureBridge is ready to preview ShitBot QQ images, GIFs and expressions in chat.");
    }
}
