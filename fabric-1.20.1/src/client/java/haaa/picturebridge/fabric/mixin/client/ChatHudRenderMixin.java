package haaa.picturebridge.fabric.mixin.client;

import haaa.picturebridge.fabric.client.ChatEmojiManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChatHud.class)
abstract class ChatHudRenderMixin {
    @Redirect(
            method = "render(Lnet/minecraft/client/gui/DrawContext;III)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;III)I"
            )
    )
    private int picturebridge$renderInlineMedia(DrawContext context,
                                                 TextRenderer textRenderer,
                                                 OrderedText text,
                                                 int x,
                                                 int y,
                                                 int color) {
        float opacity = (color >>> 24 & 0xFF) / 255.0F;
        ChatEmojiManager.INSTANCE.renderLine(context, textRenderer, text, x, y, opacity);
        return context.drawTextWithShadow(textRenderer, text, x, y, color);
    }
}
