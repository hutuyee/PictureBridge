package haaa.picturebridge.forge.modern;

import com.mojang.blaze3d.platform.NativeImage;
import haaa.picturebridge.forge.common.DecodedImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;

final class AnimatedForgeTexture implements AutoCloseable {
    private final Minecraft minecraft;private final DecodedImage decoded;private final DynamicTexture texture;private final ResourceLocation location;
    private int frame;private long next;
    AnimatedForgeTexture(Minecraft minecraft,DecodedImage decoded){this.minecraft=minecraft;this.decoded=decoded;texture=new DynamicTexture(decoded.width(),decoded.height(),true);upload(decoded.frames().get(0).image());location=minecraft.getTextureManager().register("picturebridge",texture);next=System.nanoTime()+decoded.frames().get(0).durationMillis()*1_000_000L;}
    ResourceLocation location(){return location;}int width(){return decoded.width();}int height(){return decoded.height();}int frames(){return decoded.frames().size();}boolean animated(){return decoded.animated();}
    void update(long now){if(!decoded.animated()||now<next)return;int n=0;do{frame=(frame+1)%decoded.frames().size();next+=decoded.frames().get(frame).durationMillis()*1_000_000L;n++;}while(now>=next&&n<decoded.frames().size());upload(decoded.frames().get(frame).image());}
    private void upload(BufferedImage image){NativeImage pixels=texture.getPixels();if(pixels==null)return;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)pixels.setPixelRGBA(x,y,abgr(image.getRGB(x,y)));texture.upload();}
    private static int abgr(int c){return c&0xFF00FF00|c>>16&0xFF|(c&0xFF)<<16;}
    @Override public void close(){minecraft.getTextureManager().release(location);}
}
