package haaa.picturebridge.fabric.mixin.client;

import haaa.picturebridge.fabric.client.ChatEmojiManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.hud.ChatHud$Interactable")
abstract class ChatHudInteractableBackendMixin {
    @Shadow
    @Final
    private DrawContext context;

    @Inject(method = "text", at = @At("HEAD"))
    private void picturebridge$renderInlineExpressions(int y,
                                                       float opacity,
                                                       OrderedText text,
                                                       CallbackInfoReturnable<Boolean> callback) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatEmojiManager.INSTANCE.renderLine(context, client.textRenderer, text, y, opacity);
    }
}
