package stress;

import irc.Sentence;
import irc.SentenceImpl;
import jvn.JvnProxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Stresser implements Runnable {
    private int clientId;
    private int objectsAmount;
    private Thread t;

    Stresser(int clientId, int objectsAmount) {
        this.clientId = clientId;
        this.objectsAmount = objectsAmount;
    }

    private int getRandomInt(int max) {
        return new Random().nextInt(max);
    }

    private int getRandomInt(int min, int max) {
        return new Random().nextInt((max - min) + 1) + min;
    }

    public void run() {
        ArrayList<Sentence> jvnObjects = new ArrayList<>();
        for (int i = 0; i < objectsAmount; i++) {
            try {
                String n = String.format("jvnObject-stress-%d", i);
                jvnObjects.add((Sentence) JvnProxy.get(n, new SentenceImpl()));
                Thread.sleep(250);
            } catch (Exception e) {
                System.err.println("Stresser problem: " + e.getMessage());
            }
        }

        while (true) {
            try {
                Thread.sleep(getRandomInt(100, 1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int readOrWrite = getRandomInt(2);
            int jvnObjectNumber = getRandomInt(objectsAmount);
            switch (readOrWrite) {
                case 0:
                    // read
                    String read = jvnObjects.get(jvnObjectNumber).read();
                    String readMessage = String.format("Reading jvnObject n°%d. Value read: \"%s\"", jvnObjectNumber, read);
                    log(readMessage);
                    break;
                case 1:
                    // write
                    String stuff = String.format("stuff %d %d", jvnObjectNumber, clientId);
                    jvnObjects.get(jvnObjectNumber).write(stuff);
                    String writeMessage = String.format("Writing stuff to jvnObject n°%d", jvnObjectNumber);
                    log(writeMessage);
                    break;
            }
        }
    }

    void start() {
        if (t == null) {
            String threadName = "" + clientId;
            t = new Thread(this, threadName);
            t.start();
        }
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final ArrayList<String> COLORS = new ArrayList<>(
            Arrays.asList(
                    ANSI_RED,
                    ANSI_GREEN,
                    ANSI_YELLOW,
                    ANSI_BLUE,
                    ANSI_PURPLE,
                    ANSI_CYAN,
                    ANSI_WHITE
            )
    );

    private void log(String message) {
        String color = COLORS.get(clientId % COLORS.size());
        String prefix = String.format("%sstresser-%d:%s ", color, clientId, ANSI_RESET);
        System.out.println(prefix + message);
    }
}
