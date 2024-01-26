import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class AdHocNetworkSimulation {
    public static final String MULTICAST_GROUP = "224.0.0.1";
    public static final int PORT = 8888;
    public static int MAX_NODES = 50;
    public static final int MULTI_MSG = 2;
    public static final int NUM_THREADS = 5;
    public static final double LOSS_PROB = 0.05; // 0 a 1
    public static double NODE_DIST = 10; // metros
    public static final double TIME_FACTOR = 0.1; // milissegundos
    public static final double DIST_FACTOR = 0.0005; // 100m -> P(erro)=1
    public static FileOutputStream fileOut = null;
    public static PrintStream printOut;

    public static void main(String args[]) {
        try {
            fileOut = new FileOutputStream("log.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        printOut = new PrintStream(fileOut);
        System.setOut(printOut);

        InetAddress multicastGroup = null;
        try {
            multicastGroup = InetAddress.getByName(MULTICAST_GROUP);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        Node[] nodes = new Node[MAX_NODES];
        for (int i = 0; i < MAX_NODES; i++) {
            nodes[i] = new Node(i + 1, multicastGroup);
            nodes[i].startReceiverThread();
        }

        for (int j = 0; j < MULTI_MSG; j++) {
            for (int i = 0; i < MAX_NODES; i += NUM_THREADS) {
                List<Thread> sendThreads = new ArrayList<>();
                for (int k = 0; k < NUM_THREADS && (i + k) < MAX_NODES; k++) {
                    final int index = i + k;

                    String message = "Hello from Node " + (index + 1);
                    System.out.println("Node " + (index + 1) + " sent: " + message);

                    Thread sendThread = new Thread(() -> {
                        nodes[index].sendMessage(message);
                    });

                    sendThreads.add(sendThread);
                }
                try {
                    for (Thread sendThread : sendThreads) {
                        sendThread.start();
                    }
                    for (Thread sendThread : sendThreads) {
                        sendThread.join();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        int msg_sent = MAX_NODES * MAX_NODES * MULTI_MSG;
        int msg_received = 0;
        double total_delay = 0;
        int node_received = 0;

        // Imprimir o número total de mensagens recebidas por cada nó
        System.out.println("\n");
        for (int i = 0; i < MAX_NODES; i++) {
            int msg_count = nodes[i].getMessagesReceivedCount();
            double node_delay = nodes[i].getAverageDelay();

            msg_received += msg_count;
            total_delay += node_delay;

            if (node_delay != 0) {
                node_received++;
            }
            System.out.println("Node " + (i + 1) + " received " + msg_count + " messages. Delay = " + node_delay + " ms");
        }

        System.out.println("\n\nNumber of sent messages: " + msg_sent);
        System.out.println("Number of received messages: " + msg_received);
        System.out.println("Number of lost messages: " + (msg_sent - msg_received));

        System.out.println("\nAverage delay: " + (total_delay / node_received));

        printOut.close();
        try {
            fileOut.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}