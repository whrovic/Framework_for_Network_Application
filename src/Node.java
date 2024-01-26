import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class Node {
    private int nodeId;
    private RealMulticastSocket socket;
    private InetAddress multicastGroup;
    private int messagesReceivedCount;  // Contador de mensagens recebidas
    private double delay = 0;
    private double totalDelay = 0;

    public Node(int nodeId, InetAddress multicastGroup) {
        this.nodeId = nodeId;
        this.messagesReceivedCount = 0;  // Inicializa o contador

        try {
            this.multicastGroup = multicastGroup;
            this.socket = new RealMulticastSocket(AdHocNetworkSimulation.PORT, AdHocNetworkSimulation.LOSS_PROB,
                    AdHocNetworkSimulation.NODE_DIST, AdHocNetworkSimulation.TIME_FACTOR,
                    AdHocNetworkSimulation.DIST_FACTOR);
            this.socket.joinGroup(multicastGroup);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        try {
            long sendTime = System.currentTimeMillis();
            String timedMessage = sendTime + ":" + message;
            DatagramPacket packet = new DatagramPacket(timedMessage.getBytes(), timedMessage.length(), this.multicastGroup, 8888);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startReceiverThread() {
        Thread receiverThread = new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                while(true) {
                    socket.receive(packet);
                    String receivedMessage = new String(packet.getData(), 0, packet.getLength());

                    long receivedTime = System.currentTimeMillis();
                    int separatorIndex = receivedMessage.indexOf(":");
                    long sendTime = Long.parseLong(receivedMessage.substring(0, separatorIndex));
                    String originalMessage = receivedMessage.substring(separatorIndex + 1);

                    synchronized (this) {
                        this.delay = receivedTime - sendTime;
                        this.totalDelay += delay;
                        this.messagesReceivedCount++;
                    }
                    System.out.println("Node " + nodeId + " received: " + originalMessage + ". Delay: " + delay);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiverThread.start();
    }

    public synchronized int getMessagesReceivedCount() {
        return messagesReceivedCount;
    }

    public synchronized double getAverageDelay() {
        if (messagesReceivedCount == 0) {
            return 0;
        } else {
            return totalDelay / messagesReceivedCount;
        }
    }
}