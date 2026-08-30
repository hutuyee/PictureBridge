package haaa.picturebridge.forge.modern;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.Locale;

public final class ShitBotImageLink {
    private ShitBotImageLink() {}
    public static URI find(Style style) {
        ClickEvent click=style==null?null:style.getClickEvent();
        if(click==null||click.getAction()!=ClickEvent.Action.OPEN_URL)return null;
        URI uri;try{uri=URI.create(click.getValue());}catch(IllegalArgumentException e){return null;}
        if(uri.getScheme()==null||!("http".equalsIgnoreCase(uri.getScheme())||"https".equalsIgnoreCase(uri.getScheme())))return null;
        HoverEvent hover=style.getHoverEvent();Component text=hover==null?null:hover.getValue(HoverEvent.Action.SHOW_TEXT);if(text==null)return null;
        String marker=text.getString().toLowerCase(Locale.ROOT);
        return marker.contains("qq 图片")||marker.contains("qq image")||marker.contains("qq 表情")||marker.contains("qq expression")||marker.contains("qq emoji")?uri:null;
    }
}
