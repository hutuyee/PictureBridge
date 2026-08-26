package haaa.picturebridge.forge;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.net.URI;
import java.util.Locale;

final class ShitBotImageLink {
    private ShitBotImageLink(){}
    static URI find(Style style){if(style==null||style.getClickEvent()==null||style.getClickEvent().getAction()!=ClickEvent.Action.OPEN_URL)return null;URI uri;try{uri=URI.create(style.getClickEvent().getValue());}catch(IllegalArgumentException e){return null;}if(uri.getScheme()==null||!("http".equalsIgnoreCase(uri.getScheme())||"https".equalsIgnoreCase(uri.getScheme())))return null;HoverEvent hover=style.getHoverEvent();ITextComponent text=hover!=null&&hover.getAction()==HoverEvent.Action.SHOW_TEXT?hover.getValue():null;if(text==null)return null;String marker=text.getString().toLowerCase(Locale.ROOT);return marker.contains("qq 图片")||marker.contains("qq image")||marker.contains("qq 表情")||marker.contains("qq expression")||marker.contains("qq emoji")?uri:null;}
}
