package controller.runnables;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TCPListener implements Runnable, Closeable {

	private int port;
	private boolean stopped;
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private ExecutorService executor = null;
	private List<ClientListener> clientList = null;

	public TCPListener(int port) {

		this.port = port;
		this.clientList = new ArrayList<ClientListener>();
		this.executor = Executors.newCachedThreadPool();
		this.stopped = false;
	}

	@Override
	public void run() {

		try {

			serverSocket = new ServerSocket(port);

			while (!stopped) {

				socket = serverSocket.accept();
				clientList.add(new ClientListener(socket));
				executor.execute(clientList.get(clientList.size() - 1));
			}
		} catch (SocketException e) {

			// Socket closed

		} catch (IOException e) {

			System.err.println("Can not listen on port: " + port + "!");
		}
	}

	@Override
	public void close() {

		stopped = true;

		boolean isShutDown = false;
		executor.shutdown();

		try {

			isShutDown = executor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {

		} finally {

			if (!isShutDown)
				executor.shutdownNow();
		}

		for (ClientListener cl : clientList)
			if (cl != null)
				cl.close();

		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {

			}

		if (serverSocket != null)
			try {
				serverSocket.close();
			} catch (IOException e) {

			}
	}
}
