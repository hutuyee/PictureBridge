package haaa.picturebridge.forge;

import haaa.picturebridge.forge.common.DecodedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;

final class AnimatedForgeTexture implements AutoCloseable {
    private final Minecraft mc;private final DecodedImage decoded;private final DynamicTexture texture;private final ResourceLocation location;private int frame;private long next;
    AnimatedForgeTexture(Minecraft mc,DecodedImage decoded){this.mc=mc;this.decoded=decoded;texture=new DynamicTexture(decoded.frames().get(0).image());location=mc.getTextureManager().getDynamicTextureLocation("picturebridge",texture);next=System.nanoTime()+decoded.frames().get(0).durationMillis()*1_000_000L;}
    ResourceLocation location(){return location;}int width(){return decoded.width();}int height(){return decoded.height();}int frames(){return decoded.frames().size();}boolean animated(){return decoded.animated();}
    void update(long now){if(!decoded.animated()||now<next)return;int n=0;do{frame=(frame+1)%decoded.frames().size();next+=decoded.frames().get(frame).durationMillis()*1_000_000L;n++;}while(now>=next&&n<decoded.frames().size());upload(decoded.frames().get(frame).image());}
    private void upload(BufferedImage image){int[] pixels=texture.getTextureData();image.getRGB(0,0,image.getWidth(),image.getHeight(),pixels,0,image.getWidth());texture.updateDynamicTexture();}
    @Override public void close(){mc.getTextureManager().deleteTexture(location);}
}
