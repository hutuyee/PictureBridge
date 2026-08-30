package haaa.picturebridge.neoforge.mixin;

import haaa.picturebridge.neoforge.ImageViewerScreen;
import haaa.picturebridge.neoforge.ShitBotImageLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void picturebridge$openShitBotImage(Style style,
                                                CallbackInfoReturnable<Boolean> callback) {
        URI imageUri = ShitBotImageLink.find(style);
        if (imageUri == null) {
            return;
        }
        Minecraft.getInstance().setScreen(new ImageViewerScreen((Screen) (Object) this, imageUri));
        callback.setReturnValue(true);
    }
}
