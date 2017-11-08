
/**
 * FastFtp Class
 *
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import cpsc441.a3.shared.*;

public class FastFtp {

	/**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back_N in terms of segments
     * @param rtoTimer		The time-out interval for the retransmission timer
     */
	public FastFtp(int windowSize, int rtoTimer) {
		// to be completed
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
		Socket tcp;
		DatagramSocket udp;
		


		try {
			Path path = Paths.get(System.getProperty("user.dir"), fileName);
			File file = path.toFile();

			int localPort = 4321;
			InetAddress IPAddress = InetAddress.getByName(serverName);
			tcp = new Socket(IPAddress, serverPort);	
			udp = new DatagramSocket();

			DataInputStream is = new DataInputStream(tcp.getInputStream());
			DataOutputStream os = new DataOutputStream(tcp.getOutputStream());

			os.writeUTF(fileName);
			os.writeLong(file.length());
			os.writeInt(localPort);
			int destinationPort = is.readInt();

			Queue<Segment> sendQ = new LinkedList<>();

			byte[] fileContent = new byte[(int)file.length()];
			FileInputStream fin = new FileInputStream(file);
			fin.read(fileContent);
			System.out.println(fileContent.length);
			int numChunks =(int)fileContent.length / (int) Segment.MAX_PAYLOAD_SIZE;
			int remainder =(int)fileContent.length % (int) Segment.MAX_PAYLOAD_SIZE;

			int i = 0;
			for (i = 0; i < numChunks + 1; i++){
				int start = i * Segment.MAX_PAYLOAD_SIZE;
				int end = start + Segment.MAX_PAYLOAD_SIZE;
				int nextSeqNum = i * (Segment.MAX_SEGMENT_SIZE);
				byte[] payload;
				if (i < numChunks){
					payload = Arrays.copyOfRange(fileContent, start, end);
				} else {
					payload = Arrays.copyOfRange(fileContent, start, start+remainder);
				}
				Segment segment = new Segment(nextSeqNum, payload);
				sendQ.add(segment);
			}
			System.out.println(sendQ);


			
		} catch (Exception e) {
			//TODO: handle exception
		} finally {

		}

		


		/**
		 * 1. Open TCP to server,
		 * 2. Open UDP Socket _u
		 * 3. Complete Handshake
		 * 	(a) writeUTF(): to send the name of the file to be transmitted
		 * 	(b) writeLong(): to send the length (in bytes) of the file to be transmitted
		 * 	(c) writeInt(): to send the local UDP port number used for file transfer
		 *  (d) flush()
		 *  (e) readInt(): to receive the server UDP port number used for file transfer
		 * 4. Send the file segment by segment over UDP to _r
		 * 5. Clean up and close TCP and UDP sockets
		 */
	}
	public synchronized void processSend(Segment seg) {
		// send seg to the UDP socket
		// add seg to the transmission queue
		// if this is the first segment in transmission queue, start the timer
	}
	public synchronized void processACK(Segment ack) {
		// if ACK not in the current window, do nothing
		// otherwise:
		// cancel the timer
		// remove all segements that are acked by this ACK from the transmission queue
		// if there are any pending segments in transmission queue, start the timer
	}
	public synchronized void processTimeout() {
		// get the list of all pending segments from the transmission queue
		// go through the list and send all segments to the UDP socket
		// if there are any pending segments in transmission queue, start the timer
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
