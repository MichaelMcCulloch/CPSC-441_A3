import cpsc441.a3.shared.*;

/**
 * ReceivingACK
 */
public class ReceivingACK implements Runnable  {
    private TxQueue txQ;

    public ReceivingACK(TxQueue queue) {
        txQ = queue;
    }
    
    @Override
    public void run() {
        
    }
}