package haaa.picturebridge.fabric.legacy;

import haaa.picturebridge.forge.common.DecodedImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;

final class AnimatedFabricTexture implements AutoCloseable {
    private final MinecraftClient client; private final DecodedImage decoded;
    private final NativeImageBackedTexture texture; private final Identifier identifier;
    private int frameIndex; private long nextFrameAt;
    AnimatedFabricTexture(MinecraftClient client, DecodedImage decoded) {
        this.client=client; this.decoded=decoded;
        texture=new NativeImageBackedTexture(new NativeImage(decoded.width(),decoded.height(),true));
        upload(decoded.frames().get(0).image()); identifier=client.getTextureManager().registerDynamicTexture("picturebridge",texture);
        nextFrameAt=System.nanoTime()+decoded.frames().get(0).durationMillis()*1_000_000L;
    }
    Identifier identifier(){return identifier;} int width(){return decoded.width();} int height(){return decoded.height();}
    int frameCount(){return decoded.frames().size();} boolean animated(){return decoded.animated();}
    void update(long now){
        if(!decoded.animated()||now<nextFrameAt)return; int advances=0;
        do{frameIndex=(frameIndex+1)%decoded.frames().size();nextFrameAt+=decoded.frames().get(frameIndex).durationMillis()*1_000_000L;advances++;}
        while(now>=nextFrameAt&&advances<decoded.frames().size()); upload(decoded.frames().get(frameIndex).image());
    }
    private void upload(BufferedImage frame){NativeImage pixels=texture.getImage();if(pixels==null)return;
        for(int y=0;y<frame.getHeight();y++)for(int x=0;x<frame.getWidth();x++)pixels.setColor(x,y,abgr(frame.getRGB(x,y)));texture.upload();}
    private static int abgr(int c){return c&0xFF00FF00|c>>16&0xFF|(c&0xFF)<<16;}
    @Override public void close(){client.getTextureManager().destroyTexture(identifier);}
}
