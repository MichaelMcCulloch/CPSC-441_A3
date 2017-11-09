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
    
    @Override
    public void run() {
        while (true){
            byte[] rec = new byte[1024];
            DatagramPacket p = new DatagramPacket(rec, rec.length);
            try {
                udp.receive(p);
                System.out.println("SOMETHING HAPPENED");
            } catch (Exception e) {}
        }
    }
}