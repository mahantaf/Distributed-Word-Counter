package Events;

import se.sics.kompics.KompicsEvent;

import java.util.ArrayList;

public class MapMessage implements KompicsEvent {

    public String dst;
    public String src;
    public String id;
    public ArrayList<String> lines;

    public MapMessage( String src, String dst , String id, ArrayList<String> lines) {
        this.dst = dst;
        this.src = src;
        this.id = id;
        this.lines = new ArrayList<>(lines);
    }
}
