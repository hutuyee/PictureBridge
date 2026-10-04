package haaa.picturebridge.fabric.legacy.mixin;

import haaa.picturebridge.fabric.legacy.ImageViewerScreen;
import haaa.picturebridge.fabric.legacy.ShitBotImageLink;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {
    @Shadow protected MinecraftClient minecraft;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void picturebridge$openImage(double mouseX, double mouseY, int button,
                                         CallbackInfoReturnable<Boolean> callback) {
        if (button != 0 || minecraft == null) return;
        Text component = minecraft.inGameHud.getChatHud().getText(mouseX, mouseY);
        URI uri = component == null ? null : ShitBotImageLink.find(component.getStyle());
        if (uri == null) return;
        minecraft.openScreen(new ImageViewerScreen((Screen) (Object) this, uri));
        callback.setReturnValue(true);
    }
}
