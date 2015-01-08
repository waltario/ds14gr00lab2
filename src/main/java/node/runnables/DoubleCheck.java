//thread class stage1

package node.runnables;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class DoubleCheck implements Runnable, Closeable {

	String addy;
	int port;
	
	PacketSender sender;
	int phase;
	int nRes;
	
	Socket socket;
	BufferedReader reader;
	PrintStream writer;
	
	public DoubleCheck(String addy, int port) {
		this.addy = addy;
		this.port = port;
	}
	
	
	@Override
	public void run() {
		try {
			// TCP Connection to existing Nodes in CloudController
			socket = new Socket(addy, port);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintStream(socket.getOutputStream(), true);
			
			
			// Phase 1 asks to share Resources, Nodes respond with !ok or !nok
			if(phase == 1) {
				writer.println("!share " + nRes);
				
				String response = reader.readLine();
				
				// report result to PacketSender
				if("!ok".equals(response)) {
					sender.addOk();
				}
				if("!nok".equals(response)) {
					sender.addNok();
				}
			}
			
			// Phase 2 tells the Nodes to finalize the changes; if nothing is committed, the changes don't affect anything
			else if (phase == 2) {
				writer.println("!commit");
			}
			
		} catch (UnknownHostException e) {
			System.err.println("UnknownHostException in DoubleCheck");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("IOException in DoubleCheck");
			e.printStackTrace();
		} finally {
			if(!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
		
		
		
	}

	@Override
	public void close() throws IOException {
		if(!socket.isClosed())
			socket.close();
		
	}
	
	public void setNRes(int res) {
		this.nRes = res;
	}
	
	public void setPacketSender(PacketSender sender) {
		this.sender = sender;
	}
	
	public void setPhase(int phase) {
		this.phase = phase;
	}

}
