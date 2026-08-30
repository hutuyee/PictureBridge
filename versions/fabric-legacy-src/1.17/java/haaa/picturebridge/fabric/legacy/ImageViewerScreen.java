package haaa.picturebridge.fabric.legacy;

import com.mojang.blaze3d.systems.RenderSystem;
import haaa.picturebridge.forge.common.DecodedImage;
import haaa.picturebridge.forge.common.ImageLoadException;
import haaa.picturebridge.forge.common.RemoteImageLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.net.URI;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

public final class ImageViewerScreen extends Screen {
    private final Screen parent; private final URI uri; private AnimatedFabricTexture texture;
    private String error=""; private int generation; private boolean started,dragging;
    private double zoom=1,panX,panY; private long copiedUntil,lastClick;
    public ImageViewerScreen(Screen parent,URI uri){super(new TranslatableText("picturebridge.screen.title"));this.parent=parent;this.uri=uri;}
    @Override protected void init(){int w=Math.max(60,Math.min(96,(width-32)/3)),x=(width-w*3-10)/2,y=Math.max(0,height-27);
        addDrawableChild(new ButtonWidget(x,y,w,20,new TranslatableText("picturebridge.button.back"),b->client.setScreen(parent)));
        addDrawableChild(new ButtonWidget(x+w+5,y,w,20,new TranslatableText("picturebridge.button.reload"),b->load(true)));
        addDrawableChild(new ButtonWidget(x+(w+5)*2,y,w,20,new TranslatableText("picturebridge.button.copy_url"),b->copy()));
        if(!started){started=true;load(false);}}
    @Override public void render(MatrixStack m,int mx,int my,float d){fill(m,0,0,width,height,0xC0101115);Area a=area();
        fill(m,a.l,a.t,a.r,a.b,0xB0101115);border(m,a,0xFF3A3D46);
        if(texture!=null)drawImage(m,a);else drawCenteredText(m,textRenderer,error.isEmpty()?tr("picturebridge.status.loading",dots()):error,a.cx(),a.cy()-4,error.isEmpty()?0xD9E2F2:0xFF6B6B);
        drawCenteredText(m,textRenderer,title,width/2,8,0xFFFFFF);
        drawCenteredText(m,textRenderer,tr(System.nanoTime()<copiedUntil?"picturebridge.status.copied":"picturebridge.status.hint"),width/2,Math.max(0,height-45),0xA0A7B4);
        super.render(m,mx,my,d);}
    private void drawImage(MatrixStack m,Area a){texture.update(System.nanoTime());double s=Math.min((a.w()-4D)/texture.width(),(a.h()-4D)/texture.height())*zoom;
        int w=Math.max(1,(int)Math.round(texture.width()*s)),h=Math.max(1,(int)Math.round(texture.height()*s));int x=(int)Math.round(a.cx()-w/2D+panX),y=(int)Math.round(a.cy()-h/2D+panY);
        int gs=Math.max(1,(int)client.getWindow().getScaleFactor());GL11.glEnable(GL11.GL_SCISSOR_TEST);GL11.glScissor((a.l+1)*gs,(height-a.b+1)*gs,Math.max(1,a.w()-2)*gs,Math.max(1,a.h()-2)*gs);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);RenderSystem.setShaderTexture(0,texture.identifier());RenderSystem.setShaderColor(1,1,1,1);
        drawTexture(m,x,y,0F,0F,w,h,texture.width(),texture.height());GL11.glDisable(GL11.GL_SCISSOR_TEST);
        String status=texture.animated()?tr("picturebridge.status.ready_animated",texture.width(),texture.height(),texture.frameCount(),Math.round(zoom*100)):tr("picturebridge.status.ready",texture.width(),texture.height(),Math.round(zoom*100));
        textRenderer.drawWithShadow(m,status,width-textRenderer.getWidth(status)-8,8,0xB8C7D9);}
    @Override public boolean mouseClicked(double x,double y,int b){if(super.mouseClicked(x,y,b))return true;if(texture!=null&&b==0&&area().contains(x,y)){long n=System.nanoTime();if(n-lastClick<250_000_000L)reset();else dragging=true;lastClick=n;return true;}return false;}
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy){if(dragging&&texture!=null&&b==0){panX+=dx;panY+=dy;return true;}return super.mouseDragged(x,y,b,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int b){dragging=false;return super.mouseReleased(x,y,b);}
    @Override public boolean mouseScrolled(double x,double y,double amount){if(texture!=null&&amount!=0&&area().contains(x,y)){zoom=Math.max(.1,Math.min(12,zoom*Math.pow(1.2,amount)));return true;}return super.mouseScrolled(x,y,amount);}
    @Override public boolean keyPressed(int key,int scan,int mods){if(texture!=null&&key==GLFW.GLFW_KEY_R){reset();return true;}return super.keyPressed(key,scan,mods);}
    @Override public void removed(){generation++;destroy();super.removed();}
    private void load(boolean refresh){final int req=++generation;error="";reset();destroy();RemoteImageLoader.INSTANCE.loadImage(uri,refresh).whenComplete((decoded,t)->client.execute(()->finish(req,decoded,t)));}
    private void finish(int req,DecodedImage decoded,Throwable t){if(req!=generation)return;if(t!=null||decoded==null)error=errorText(t);else texture=new AnimatedFabricTexture(client,decoded);}
    private void copy(){client.keyboard.setClipboard(uri.toASCIIString());copiedUntil=System.nanoTime()+2_000_000_000L;}
    private void destroy(){if(texture!=null)texture.close();texture=null;}private void reset(){zoom=1;panX=panY=0;}private Area area(){return new Area(8,25,Math.max(9,width-8),Math.max(26,height-51));}
    private static String tr(String k,Object...a){return I18n.translate(k,a);}private static String dots(){int n=(int)(System.currentTimeMillis()/350%4);return n==0?"":n==1?".":n==2?"..":"...";}
    private static String errorText(Throwable t){Throwable c=t;while((c instanceof CompletionException||c instanceof ExecutionException)&&c.getCause()!=null)c=c.getCause();if(c instanceof ImageLoadException){ImageLoadException e=(ImageLoadException)c;return tr(e.translationKey(),e.arguments());}return tr("picturebridge.error.network",c==null||c.getMessage()==null?"unknown error":c.getMessage());}
    private static void border(MatrixStack m,Area a,int c){fill(m,a.l,a.t,a.r,a.t+1,c);fill(m,a.l,a.b-1,a.r,a.b,c);fill(m,a.l,a.t,a.l+1,a.b,c);fill(m,a.r-1,a.t,a.r,a.b,c);}
    private static final class Area{final int l,t,r,b;Area(int l,int t,int r,int b){this.l=l;this.t=t;this.r=r;this.b=b;}int w(){return r-l;}int h(){return b-t;}int cx(){return l+w()/2;}int cy(){return t+h()/2;}boolean contains(double x,double y){return x>=l&&x<r&&y>=t&&y<b;}}
}
