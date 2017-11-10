
/**
 * FastFtp Class
 *
 */

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;


import cpsc441.a3.shared.*;

public class FastFtp {

	private volatile TxQueue txQ;
	private Socket tcp;
	private DatagramSocket udp;
	private InetAddress ipAddress;
	private int destinationPort;
	private ScheduledThreadPoolExecutor scheduler;
	private Runnable timeOutHandler;
	private ScheduledFuture<?> schedFut;

	private int timeout;
	/**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back_N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {
		// to be completed
		txQ = new TxQueue(windowSize);
		timeout = rtoTimer;
		timeOutHandler = new Runnable(){
			@Override
			public void run() {
				processTimeout();
			}
		};
		scheduler = new ScheduledThreadPoolExecutor(1);
		scheduler.setRemoveOnCancelPolicy(true);
		

	}
	/**
	 * Preprocess the file into segments of maximum size
	 * 
	 * @param filename		Name of the file to be trasferred to the rmeote server.
	 * @throws IOException 	If the file is not found in the working directory.
	 * @return 				The queue of segments which make up the file
	 */
	public Queue<Segment> segmentFile(String fileName) throws FileNotFoundException, IOException{
		int payloadSize = 64;//Segment.MAX_PAYLOAD_SIZE;
		
		Path path = Paths.get(System.getProperty("user.dir"), fileName);
		File file = path.toFile();
		Queue<Segment> sendQ = new LinkedList<>();
		
		byte[] fileContent = new byte[(int)file.length()];
		FileInputStream fin = new FileInputStream(file);
		fin.read(fileContent);
		System.out.println(fileContent.length);
		int numChunks =(int)fileContent.length / payloadSize;
		int remainder =(int)fileContent.length % payloadSize;

		int i = 0;
		for (i = 0; i < numChunks + 1; i++){
			int start = i * payloadSize;
			int end = (i < numChunks) ? start + payloadSize : start + remainder;
			int nextSeqNum = i;
			byte[] payload;
			payload = Arrays.copyOfRange(fileContent, start, end);

			Segment segment = new Segment(nextSeqNum, payload);
			sendQ.add(segment);
		}
		fin.close();
		return sendQ;
	}


    /**
     * Sends the specified file to the specified destination host:
     * 1. send file/connection infor over TCP
     * 2. start receving thread to process coming ACKs
     * 3. send file segment by segment
     * 4. wait until transmit queue is empty, i.e., all segments are ACKed
     * 5. clean up (cancel timer, interrupt receving thread, close sockets/files)
     * 
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName		Name of the file to be trasferred to the rmeote server
     */
	public void send(String serverName, int serverPort, String fileName) {
		// to be completed
		
		
		try {
			//Get the file so we can record it's length
			Path path = Paths.get(System.getProperty("user.dir"), fileName);
			File file = path.toFile(); 
			
			//Get address of the remote and local port
		 	ipAddress = InetAddress.getByName(serverName);
			tcp = new Socket(ipAddress, serverPort);	
			udp = new DatagramSocket();
			int localPort = udp.getLocalPort();

			DataInputStream is = new DataInputStream(tcp.getInputStream());
			DataOutputStream os = new DataOutputStream(tcp.getOutputStream());

			os.writeUTF(fileName);
			os.writeLong(file.length());
			os.writeInt(localPort);
			os.flush();
			destinationPort = is.readInt();
			is.close();
			os.close();

			//break the file into segments
			Queue<Segment> sendQ = segmentFile(fileName);

			Thread ackReceiver = new Thread(new ReceivingACK(this, txQ, udp, sendQ.size() - 1));
			ackReceiver.start();

			while (!sendQ.isEmpty()){
				
				while (isTxQFull(txQ)) {}
				Segment next = sendQ.remove();
				processSend(next);
			}
			
			ackReceiver.join();
			tcp.close();
			udp.close();
			scheduler.shutdown();
		} catch (FileNotFoundException e) {
			System.err.println("File " + fileName + " not found. Exiting");
		} catch (UnknownHostException e) {
			System.err.println("Host " + serverName + " not found. Exiting");
		} catch (IOException e){
			System.err.println("IOException");
		} catch (InterruptedException e) {
			System.err.println("Interrupted Exception");
		}
	}

	//reading the queue state needs to be synched in this context
	public synchronized boolean isTxQFull(TxQueue q){
		return q.isFull();
	}
	public synchronized void processSend(Segment seg) {
		DatagramPacket pkt = new DatagramPacket(seg.getBytes(), seg.getBytes().length, ipAddress, destinationPort);
		try {
			// add seg to the transmission queue
			// if this is the first segment in transmission queue, start the timer
			System.out.println("SEND:" + seg.getSeqNum());
			
			if (txQ.isEmpty()) {
				schedFut = scheduler.scheduleWithFixedDelay(timeOutHandler, timeout, timeout, TimeUnit.MILLISECONDS);
			}
			txQ.add(seg);
			udp.send(pkt);
			
			int size = txQ.size();
			
			
			
		} catch (IOException e) {
			System.err.println("Unable to send packet");
		} catch (InterruptedException e){
			System.err.println("Unable to add to queue");
		}
	}
	public synchronized void processACK(Segment ack) {
		
		try {

			int size = txQ.size();
			for ( int i = 0; i < size; i++){
				Segment next = txQ.remove();
				if (next.getSeqNum() >= ack.getSeqNum()){
					txQ.add(next);
				}
			}

			int sizeAfter = txQ.size();

			if (size != sizeAfter) {
				schedFut.cancel(false);
			} 
			if (sizeAfter != 0){
				schedFut.cancel(false);
				schedFut = scheduler.scheduleWithFixedDelay(timeOutHandler, timeout, timeout, TimeUnit.MILLISECONDS);
			}	

/*	
			if (size != sizeAfter)  //Then ACK must have been in current window, already removed above
				schedFut.cancel(false);
			

			if (sizeAfter != 0) 
				schedFut = scheduler.scheduleWithFixedDelay(timeOutHandler, timeout, timeout, TimeUnit.MILLISECONDS);
*/			
			// if ACK not in the current window, do nothing
			// otherwise:
			// TODO: TIMER CANCEL
			// remove all segements that are acked by this ACK from the transmission queue
			// TODO: if there are any pending segments in transmission queue, TIMER START

			
			
		} catch (Exception e) {
			//TODO: handle exception
		}
		
	}
	public synchronized void processTimeout() {
		System.out.println("TIMEOUT");
		try {
			// get the list of all pending segments from the transmission queue
			// go through the list and send all segments to the UDP socket
			int size = txQ.size();
			for (int i = 0; i < size; i++){
				Segment next = txQ.remove();
				DatagramPacket pkt = new DatagramPacket(next.getBytes(), next.getBytes().length, ipAddress, destinationPort);
				udp.send(pkt);
				txQ.add(next);
				System.out.println("RESEND:" + next.getSeqNum());
			}
			
			// schedFut = scheduler.schedule(timeOutHandler, timeout, TimeUnit.MILLISECONDS);
			// if there are any pending segments in transmission queue, TIMER START. lets just leave it running shall we?
		} catch (Exception e) {
			
		}
	}
	
	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		

		// all srguments should be provided
		// as described in the assignment description 
		if (args.length != 5) {
			System.out.println("incorrect usage, try again.");
			System.out.println("usage: FastFtp server port file window timeout");
			System.exit(1);
		}
		
		
		// parse the command line arguments
		// assume no errors
		String serverName = args[0];
		int serverPort = Integer.parseInt(args[1]);
		String fileName = args[2];
		int windowSize = Integer.parseInt(args[3]);
		int timeout = Integer.parseInt(args[4]);


		// send the file to server
		FastFtp ftp = new FastFtp(windowSize, timeout);
		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("file transfer completed.");
	}
}
