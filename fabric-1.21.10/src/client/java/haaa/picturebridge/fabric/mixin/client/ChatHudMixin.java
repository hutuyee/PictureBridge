package haaa.picturebridge.fabric.mixin.client;

import haaa.picturebridge.fabric.client.ExpressionText;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
abstract class ChatHudMixin {
    @ModifyVariable(
            method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private ChatHudLine picturebridge$replaceExpressionLabels(ChatHudLine line) {
        net.minecraft.text.Text content = ExpressionText.replaceExpressions(line.content());
        if (content == line.content()) {
            return line;
        }
        return new ChatHudLine(
                line.creationTick(),
                content,
                line.signature(),
                line.indicator()
        );
    }
}
