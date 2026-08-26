package haaa.picturebridge.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(
        modid = PictureBridgeForge.MOD_ID,
        name = "PictureBridge",
        version = PictureBridgeForge.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true
)
public final class PictureBridgeForge {
    public static final String MOD_ID = "picturebridge";
    public static final String VERSION = "0.4.0";

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new PictureBridgeClientEvents());
    }
}
