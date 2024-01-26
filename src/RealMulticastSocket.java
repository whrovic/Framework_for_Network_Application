import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Random;

public class RealMulticastSocket extends MulticastSocket {
    private final double lossProb;
    private final double nodeDist;
    private final double timeFactor;
    private final double distFactor;
    private boolean channelOccupied;
    private Random random;
    private String lastPacketReceived = null;

    public RealMulticastSocket(int port, double lossProb, double nodeDist, double timeFactor, double distFactor) throws IOException {
        super(port);
        this.lossProb = lossProb;
        this.nodeDist = nodeDist;
        this.timeFactor = timeFactor;
        this.distFactor = distFactor;
        this.channelOccupied = false;
        random = new Random();
    }

    public void send(DatagramPacket packet) throws IOException {
        // Simular perdas por ruído eltromagnético
        if (random.nextDouble() > lossProb) {
            // Simular perdas devido à distância
            if (isInRange()) {
                try {
                    // Simular atraso de processamento no nó de envio
                    Thread.sleep((long) (Math.abs(random.nextGaussian()) * 1 + 0.5));

                    // Simular atraso de transmissão até ao nó central
                    Thread.sleep((long) (timeFactor * nodeDist));

                    // Simular atraso de processamento no nó central
                    Thread.sleep((long) (Math.abs(random.nextGaussian()) * 5 + 0.5));

                    // Simular ocupação do canal
                    channelOccupied = true;
                    // Simular atraso de transmissão até ao nó de destino
                    Thread.sleep((long) (timeFactor * nodeDist));
                    channelOccupied = false;
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                super.send(packet);
            } else {
                System.out.println("Message lost due to distance (1)");
            }
        } else {
            System.out.println("Message lost due to noise (1)");
        }
    }

    public void receive(DatagramPacket packet) throws IOException {
        boolean receive = false;

        do {
            super.receive(packet);

            // Simular perdas por ruído eltromagnético
            if (random.nextDouble() > lossProb) {
                // Simular perdas por colisão
                if (!channelOccupied) {
                    // Simular perdas devido à distância
                    if (isInRange()) {
                        receive = true;
                    } else {
                        System.out.println("Message lost due to distance (2)");
                    }
                } else {
                    System.out.println("Message lost due to collision (2)");
                }
            } else {
                System.out.println("Message lost due to noise (2)");
            }
        } while (receive != true);
    }

    private boolean isInRange() {
        // 1 - e^(-factor * d^2)
        return random.nextDouble() > (1 - Math.exp(-distFactor * nodeDist * nodeDist));
    }
}