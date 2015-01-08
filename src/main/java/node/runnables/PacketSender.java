package node.runnables;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import node.Node;

public class PacketSender extends Thread implements Closeable {

	private DatagramSocket socket = null;
	private String host = null;
	private int udpPort;
	private int tcpPort;
	private int alivePeriod;
	private String operators;
	private boolean stopped;
	
	//variables stage 1
	private int okCount;
	private int nokCount;
	private int minRes;
	private Node node;

	public PacketSender(String host, int udpPort, int tcpPort, int alivePeriod,
			String operators) {

		this.host = host;
		this.udpPort = udpPort;
		this.tcpPort = tcpPort;
		this.alivePeriod = alivePeriod;
		this.operators = operators;
		this.stopped = false;
	}

	@Override
	public void run() {

		try {

			socket = new DatagramSocket();
			DatagramPacket packet = null;
			byte[] buf = null;

			
			// Send !Hello to CloudController
			buf = ("!hello").getBytes();
			packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(host), udpPort);
			socket.send(packet);
			
			// Receive !Init Message
			buf = new byte[1024];
			packet = new DatagramPacket(buf, buf.length);
			socket.receive(packet);
			String initMessage = new String(packet.getData()).trim(); //System.out.println(initMessage); // System.out.println("mres" + minRes);
			String[] init = initMessage.split(" ");
			ExecutorService exe = null;
			
			int nodeCount = init.length - 1;
			int mRes = Integer.parseInt(init[init.length - 1]);
			int nRes = mRes / nodeCount;
			
			okCount = 0;
			nokCount = 0;
			
			// Check if Resources suffice for self
			if(nRes < minRes) {
				//System.out.println("Not enough Resources available for this Node");
				node.setRes(0);
				stopped = true;
			}
			
			
			// If other Nodes exist, contact them
			else if(nodeCount > 1) {
				if("!init".equals(init[0])) {
					exe = Executors.newFixedThreadPool(init.length - 2);	
					
					for(int i = 1; i < nodeCount; i++) {
						String addy = init[i].split(":")[0];
						int port = Integer.parseInt(init[i].split(":")[1]); //System.out.println("phase 1 " + addy + " " + port);
						
						DoubleCheck checkThread = new DoubleCheck(addy, port);
						checkThread.setNRes(nRes);
						checkThread.setPhase(1);
						checkThread.setPacketSender(this);
						exe.execute(checkThread);
					}
				}
				
				// Wait up to 5 seconds for threads to respond, check status - the TCP threads access the ok/nok counts
				boolean ok = false;			//System.out.println("phase 1 finished, waiting for response ok/nok");
				for(int i = 0; i < 5; i++) {
					if(okCount >= nodeCount - 1) {
						ok = true;
						break;
					}
					if(nokCount > 0) {
						ok = false;
						break;
					}
					this.sleep(1000);
				}
				
				if(exe != null)
					exe.shutdown();
				
				// If changes are ok, contact Nodes again to confirm, if not ok, set flag to shut this node down
				if(ok) {
					node.setRes(nRes);
					if(init.length > 2)
						exe = Executors.newFixedThreadPool(init.length - 2);	
					
					for(int i = 1; i < nodeCount; i++) {
						String addy = init[i].split(":")[0];
						int port = Integer.parseInt(init[i].split(":")[1]);
						
						//System.out.println("phase 2 " + addy + " " + port);
						DoubleCheck checkThread = new DoubleCheck(addy, port);
						checkThread.setPhase(2);
						checkThread.setPacketSender(this);
						exe.execute(checkThread);
					}
				} else {
					node.setRes(0);
					stopped = true;
				}
			} else
				node.setRes(nRes);
			
			if(exe != null)
				exe.shutdown();
			
			if(stopped)
				System.out.println("Not enough Resources available to support this Node, can't join Cloud");
			else
				System.out.println("Successfully joined Cloud");
			
			// stage 1 finished
			
			while (!stopped) {

				buf = ("!alive " + tcpPort + " " + operators).getBytes();
				packet = new DatagramPacket(buf, buf.length,
						InetAddress.getByName(host), udpPort);
				socket.send(packet);

				sleep(alivePeriod);
			}

		} catch (InterruptedException e) {

			// End loop
		} catch (SocketException e) {

			// Socket closed
		} catch (UnknownHostException e) {

			System.err.println("Don't know about host!");
		} catch (IOException e) {

			System.err.println("Error while IO!");
		}
	}

	@Override
	public void close() {

		stopped = true;
		this.interrupt();

		if (socket != null)
			socket.close();
	}
	
	
	//helper functions stage1
	public void addOk() {
		this.okCount++;
	}
	
	public void addNok() {
		this.nokCount++;
	}
	
	public void setMinRes(int mRes) {
		this.minRes = mRes;
	}
	
	public void setNode(Node node) {
		this.node = node;
	}
}

