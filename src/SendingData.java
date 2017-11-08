import cpsc441.a3.shared.*;

/**
 * SendingData
 */
public class SendingData implements Runnable {
    private TxQueue txQ;

    public SendingData(TxQueue queue) {
        txQ = queue;
    }

    @Override
    public void run() {
        
    }
    
}