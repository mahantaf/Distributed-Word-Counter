package Components;
import Events.*;
import Ports.EdgePort;
import se.sics.kompics.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Node extends ComponentDefinition {
    Positive<EdgePort> receivePort = positive(EdgePort.class);
    Negative<EdgePort> sendPort = negative(EdgePort.class);
    Boolean isRoot = false;
    public String nodeName;
    public String parentName;
    public int shouldReceiveReduce = 0;

    public ArrayList<String> leaves = new ArrayList<>();
    HashMap<String, Integer> wordsCount = new HashMap<>();
    HashMap<String,Integer> neighbours = new HashMap<>();

    public Node(InitMessage initMessage) {
        nodeName = initMessage.nodeName;
        System.out.println("initNode :" + initMessage.nodeName);
        this.neighbours = initMessage.neighbours;
        this.isRoot = initMessage.isRoot;
        this.leaves = initMessage.leaves;

        subscribe(startHandler, control);
        subscribe(mapHandler,receivePort);
        subscribe(reduceHandler,receivePort);
    }

    private int numberOfFileLines(String filePath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        int lines = 0;
        while (reader.readLine() != null) lines++;
        reader.close();
        return lines;
    }

    public int numberOfChildren() {
        int count = neighbours.size();
        if (!isRoot)
            count--;
        return count;
    }

    private void printWordsCount() {
        for (Map.Entry<String, Integer> entry : wordsCount.entrySet())
            System.out.println(entry.getKey() + "\t\t\t" + entry.getValue());
    }

    public void writeWordsCountToFile() {
        try {
            Writer output;
            output = new BufferedWriter(new FileWriter("src/main/java/output.txt", true));
            for (Map.Entry<String, Integer> entry : wordsCount.entrySet())
                output.append(entry.getKey()).append(": ").append(String.valueOf(entry.getValue())).append("\n");
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reduce (HashMap<String, Integer> hm) {
        for (Map.Entry<String, Integer> entry : hm.entrySet()) {
            if (!wordsCount.containsKey(entry.getKey()))
                wordsCount.put(entry.getKey(), entry.getValue());
            else
                wordsCount.put(entry.getKey(), wordsCount.get(entry.getKey()) + entry.getValue());
        }
    }

    private void map(ArrayList<String> lines) {
        Pattern p = Pattern.compile("[a-zA-Z]+");
        for (String line : lines) {
            Matcher m = p.matcher(line);
            while (m.find())
            {
                String word = m.group();
                if(!wordsCount.containsKey(word))
                    wordsCount.put(word, 1);
                else
                    wordsCount.put(word, wordsCount.get(word) + 1);
            }
        }
    }

    Handler reduceHandler = new Handler<ReduceMessage>() {
        @Override
        public void handle(ReduceMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                System.out.println("Node " + nodeName + " received REDUCE message from " + event.src);
                reduce(event.hm);
                shouldReceiveReduce++;
                if(shouldReceiveReduce == numberOfChildren()) {
                    if(isRoot) {
                        System.out.println("Node " + nodeName + " is a parent node and should print the result");
                        writeWordsCountToFile();
                    } else {
                        trigger(new ReduceMessage(nodeName, parentName, wordsCount), sendPort);
                    }
                }
            }
        }
    };

    Handler mapHandler = new Handler<MapMessage>() {
        @Override
        public void handle(MapMessage event) {
            if (nodeName.equalsIgnoreCase(event.dst)) {
                parentName = event.src;
                if (nodeName.equalsIgnoreCase(event.id)) {
                    System.out.println("Node " + nodeName + " received MAP message from " + event.src);
                    // This is a mapper node and should calculate its batch of lines.
                    map(event.lines);
                    trigger(new ReduceMessage(nodeName, parentName, wordsCount), sendPort);
                } else {
                    // This is a reducer node and should pass the message to its children
                    for (Map.Entry<String, Integer> entry : neighbours.entrySet())
                        if (!entry.getKey().equalsIgnoreCase(parentName))
                            trigger(new MapMessage(nodeName, entry.getKey(), event.id, event.lines), sendPort);
                }
            }
        }
    };

    Handler startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            if (isRoot) {
                String filePath = "src/main/java/CA-text-file.txt";
                try {
                    int numberOfFileLines = numberOfFileLines(filePath);
                    double batchSize = Math.ceil(numberOfFileLines / leaves.size());

                    File resourceFile = new File(filePath);
                    Scanner scanner = new Scanner(resourceFile);
                    int candidate = 0;
                    int lineNumber = 1;
                    ArrayList<String> lines = new ArrayList<>();
                    while (scanner.hasNext() && candidate < leaves.size()) {
                        if (lineNumber % batchSize != 0)
                            lines.add(scanner.nextLine());
                        else {
                            for (Map.Entry<String, Integer> entry : neighbours.entrySet())
                                trigger(new MapMessage(nodeName, entry.getKey(), leaves.get(candidate), lines), sendPort);
                            candidate++;
                            lines.clear();
                        }
                        lineNumber++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
}

