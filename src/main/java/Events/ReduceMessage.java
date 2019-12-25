package Events;

import se.sics.kompics.KompicsEvent;
import java.util.HashMap;

public class ReduceMessage implements KompicsEvent {

    public String dst;
    public String src;
    public HashMap<String, Integer> hm;

    public ReduceMessage(String src, String dst , HashMap<String, Integer> hm) {
        this.dst = dst;
        this.src = src;
        this.hm = hm;
    }
}
