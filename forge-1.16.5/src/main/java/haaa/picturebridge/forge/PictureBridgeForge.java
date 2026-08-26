package haaa.picturebridge.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(PictureBridgeForge.MOD_ID)
public final class PictureBridgeForge {
    public static final String MOD_ID = "picturebridge";

    public PictureBridgeForge() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> MinecraftForge.EVENT_BUS.register(new PictureBridgeClientEvents()));
    }
}
