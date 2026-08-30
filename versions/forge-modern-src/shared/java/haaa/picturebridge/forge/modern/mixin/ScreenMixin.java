package haaa.picturebridge.forge.modern.mixin;

import haaa.picturebridge.forge.modern.ImageViewerScreen;
import haaa.picturebridge.forge.modern.ShitBotImageLink;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URI;

@Mixin(Screen.class)
abstract class ScreenMixin {
    @Inject(method="handleComponentClicked",at=@At("HEAD"),cancellable=true)
    private void picturebridge$openImage(Style style,CallbackInfoReturnable<Boolean> callback){
        if(!((Object)this instanceof ChatScreen))return;URI uri=ShitBotImageLink.find(style);if(uri==null)return;
        Minecraft.getInstance().setScreen(new ImageViewerScreen((Screen)(Object)this,uri));callback.setReturnValue(true);
    }
}
