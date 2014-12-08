package node.runnables;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class PacketSender extends Thread implements Closeable {

	private DatagramSocket socket = null;
	private String host = null;
	private int udpPort;
	private int tcpPort;
	private int alivePeriod;
	private String operators;
	private boolean stopped;

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
}
