package haaa.picturebridge.fabric.mixin.client;

import haaa.picturebridge.fabric.ShitBotImageLink;
import haaa.picturebridge.fabric.client.ImageViewerScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {
    @Inject(method = "handleClickEvent", at = @At("HEAD"), cancellable = true)
    private void picturebridge$openShitBotImage(Style style,
                                                boolean insert,
                                                CallbackInfoReturnable<Boolean> callback) {
        // Shift-click is vanilla's text insertion gesture and should keep its original behavior.
        if (insert) {
            return;
        }

        URI imageUri = ShitBotImageLink.find(style);
        if (imageUri == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ImageViewerScreen((Screen) (Object) this, imageUri));
        callback.setReturnValue(true);
    }
}
