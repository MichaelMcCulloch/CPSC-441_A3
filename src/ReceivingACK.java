import java.net.DatagramPacket;
import java.net.DatagramSocket;

import cpsc441.a3.shared.*;

/**
 * ReceivingACK
 */
public class ReceivingACK implements Runnable  {
    private TxQueue txQ;
    private DatagramSocket udp;

    public ReceivingACK(TxQueue queue, DatagramSocket sock) {
        txQ = queue;
        udp = sock;
    }
    
    /**
     * Something like this
     */
    @Override
    public void run() {
        byte[] response;
        while (true){
            response = new byte[4];
            DatagramPacket ackPkt = new DatagramPacket(response, 4);
            try {
                udp.receive(ackPkt);
                Segment ack = new Segment(ackPkt);
                System.out.println(ack.getSeqNum());
            } catch (Exception e) {}
        }
    }
}