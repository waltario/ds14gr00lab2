package controller.runnables;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import controller.helpers.NodeHelper;
import controller.helpers.StatsCollector;

public class UDPListener implements Runnable, Closeable {

	private int port;
	private int timeout;
	private int checkPeriod;
	private boolean stopped;
	private StatsCollector collector = null;
	private DatagramSocket socket = null;
	private ExecutorService executor = null;
	private NodeChecker nodeChecker = null;
	
	private int mRes = 0;

	public UDPListener(int port, int timeout, int checkPeriod) {

		this.port = port;
		this.timeout = timeout;
		this.checkPeriod = checkPeriod;
		collector = StatsCollector.getInstance();
		executor = Executors.newFixedThreadPool(1);
	}

	@Override
	public void run() {

		String received = null;
		byte[] buf = new byte[1024];

		nodeChecker = new NodeChecker(timeout, checkPeriod);
		executor.execute(nodeChecker);

		try {

			socket = new DatagramSocket(port);

			while (!stopped) {

				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);

				received = new String(packet.getData()).trim();
				manageReceived(packet.getAddress(), packet.getPort(), received);
			}

		} catch (SocketException e) {

			// Socket closed;
			stopped = true;
		} catch (IOException e) {

			System.err.println("Can not listen on port: " + port);
			stopped = true;
		}
	}

	private void manageReceived(InetAddress addr, int port, String received) {

		String[] parts = received.split(" ");

		if (parts.length == 3 && "!alive".equals(parts[0])) {

			try {

				int nodePort = Integer.valueOf(parts[1]);
				String operators = parts[2].trim();

				// System.err.println("<<<<<<<<" + operators + ">>>>>>>>");

				boolean check = collector.existsNode(nodePort);

				if (!check) {

					NodeHelper node = new NodeHelper(addr, nodePort, operators);
					collector.addNode(node);
				} else {

					collector.updateNode(nodePort, true);
				}

			} catch (NumberFormatException e) {
				return;
			}
		}
		
		// stage 1 receive !hello, send !init
		else if ("!hello".equals(parts[0])) {
			DatagramPacket packet = null;
			byte[] buf = null;
			
			StatsCollector statsColle = StatsCollector.getInstance();
			ConcurrentHashMap<Integer, NodeHelper> nodeMap = statsColle.getNodeMap();
			StringBuilder sb = new StringBuilder();
			Collection<NodeHelper> list = nodeMap.values();

			for (NodeHelper o : list) {
				if(o.isOnline()) {
					sb.append(o.getAddress().getHostAddress() + ":" + o.getPort() + " ");
				}
			}
			buf = ("!init " + sb.toString() + mRes).getBytes();
			packet = new DatagramPacket(buf, buf.length, addr, port);

			try {
				socket.send(packet);
			} catch (IOException e) {
				System.out.println("send packet fail 1");
			}
		}
	}

	@Override
	public void close() {

		stopped = true;

		executor.shutdown();

		try {

			executor.awaitTermination(1, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
		} finally {

			executor.shutdownNow();
		}

		if (nodeChecker != null)
			nodeChecker.close();

		if (socket != null)
			socket.close();
	}
	
	//stage 1 helper method
	public void setRes(int res) {
		this.mRes = res;
	}
}
