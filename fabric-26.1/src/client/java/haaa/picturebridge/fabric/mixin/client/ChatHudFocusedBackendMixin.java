package haaa.picturebridge.fabric.mixin.client;

import haaa.picturebridge.fabric.client.ChatEmojiManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess")
abstract class ChatHudFocusedBackendMixin {
    @Shadow
    @Final
    private GuiGraphicsExtractor graphics;

    @Inject(method = "handleMessage", at = @At("HEAD"))
    private void picturebridge$renderInlineMedia(int y,
                                                 float opacity,
                                                 FormattedCharSequence text,
                                                 CallbackInfoReturnable<Boolean> callback) {
        Minecraft minecraft = Minecraft.getInstance();
        ChatEmojiManager.INSTANCE.renderLine(graphics, minecraft.font, text, y, opacity);
    }
}
