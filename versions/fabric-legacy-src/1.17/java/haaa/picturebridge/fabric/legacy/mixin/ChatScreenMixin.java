package haaa.picturebridge.fabric.legacy.mixin;

import haaa.picturebridge.fabric.legacy.ImageViewerScreen;
import haaa.picturebridge.fabric.legacy.ShitBotImageLink;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;

@Mixin(Screen.class)
abstract class ChatScreenMixin {
    @Inject(method = "handleTextClick", at = @At("HEAD"), cancellable = true)
    private void picturebridge$openImage(Style style, CallbackInfoReturnable<Boolean> callback) {
        if (!((Object) this instanceof ChatScreen)) return;
        URI uri = ShitBotImageLink.find(style);
        if (uri == null) return;
        MinecraftClient.getInstance().setScreen(new ImageViewerScreen((Screen) (Object) this, uri));
        callback.setReturnValue(true);
    }
}
