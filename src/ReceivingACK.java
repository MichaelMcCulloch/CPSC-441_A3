import java.net.DatagramPacket;
import java.net.DatagramSocket;

import cpsc441.a3.shared.*;

/**
 * ReceivingACK
 */
public class ReceivingACK implements Runnable  {
    private TxQueue txQ;
    private DatagramSocket udp;
    private FastFtp main;
    private int last;

    public ReceivingACK(FastFtp main, TxQueue queue, DatagramSocket sock, int lastSeq) {
        txQ = queue;
        udp = sock;
        this.main = main;
        last = lastSeq;
    }
    
    /**
     * Something like this
     */
    @Override
    public void run() {
        byte[] response;
        boolean quit = false;
        while (!quit){
            response = new byte[4];
            DatagramPacket ackPkt = new DatagramPacket(response, 4);
            try {
                udp.receive(ackPkt);
                Segment ack = new Segment(ackPkt);
                System.out.println("ACK :" + ack.getSeqNum());
                main.processACK(ack);

                if (ack.getSeqNum() > last) quit = true;
                
            } catch (Exception e) {}
        }
    }
}