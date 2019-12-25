package Events;

import Components.Node;
import se.sics.kompics.Init;

import java.util.ArrayList;
import java.util.HashMap;

public class InitMessage extends Init<Node> {
    public String nodeName;
    public boolean isRoot = false;
    public HashMap<String,Integer> neighbours = new HashMap<>();
    public ArrayList<String> leaves = new ArrayList<>();

    public InitMessage(String nodeName, boolean isRoot,
                       HashMap<String,Integer> neighbours, ArrayList<String> leaves) {
        this.nodeName = nodeName;
        this.isRoot = isRoot;
        this.neighbours = neighbours;
        this.leaves = leaves;
    }
}