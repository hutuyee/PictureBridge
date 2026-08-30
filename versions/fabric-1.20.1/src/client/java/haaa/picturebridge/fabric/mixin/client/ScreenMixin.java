package haaa.picturebridge.fabric.mixin.client;

import haaa.picturebridge.fabric.ShitBotImageLink;
import haaa.picturebridge.fabric.client.ImageViewerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true)
    private void picturebridge$openShitBotImage(Style style,
                                                CallbackInfoReturnable<Boolean> callback) {
        URI imageUri = ShitBotImageLink.find(style);
        if (imageUri == null) {
            return;
        }

        MinecraftClient.getInstance().setScreen(
                new ImageViewerScreen((Screen) (Object) this, imageUri));
        callback.setReturnValue(true);
    }
}
