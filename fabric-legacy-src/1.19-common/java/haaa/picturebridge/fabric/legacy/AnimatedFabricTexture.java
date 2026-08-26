package haaa.picturebridge.fabric.legacy;

import haaa.picturebridge.forge.common.DecodedImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;

final class AnimatedFabricTexture implements AutoCloseable {
    private final MinecraftClient client;private final DecodedImage decoded;private final NativeImageBackedTexture texture;private final Identifier id;
    private int frame;private long next;
    AnimatedFabricTexture(MinecraftClient client,DecodedImage decoded){this.client=client;this.decoded=decoded;texture=new NativeImageBackedTexture(new NativeImage(decoded.width(),decoded.height(),true));upload(decoded.frames().get(0).image());id=client.getTextureManager().registerDynamicTexture("picturebridge",texture);next=System.nanoTime()+decoded.frames().get(0).durationMillis()*1_000_000L;}
    Identifier id(){return id;}int width(){return decoded.width();}int height(){return decoded.height();}int frames(){return decoded.frames().size();}boolean animated(){return decoded.animated();}
    void update(long now){if(!decoded.animated()||now<next)return;int n=0;do{frame=(frame+1)%decoded.frames().size();next+=decoded.frames().get(frame).durationMillis()*1_000_000L;n++;}while(now>=next&&n<decoded.frames().size());upload(decoded.frames().get(frame).image());}
    private void upload(BufferedImage image){NativeImage pixels=texture.getImage();if(pixels==null)return;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)pixels.setColor(x,y,abgr(image.getRGB(x,y)));texture.upload();}
    private static int abgr(int c){return c&0xFF00FF00|c>>16&0xFF|(c&0xFF)<<16;}
    @Override public void close(){client.getTextureManager().destroyTexture(id);}
}
