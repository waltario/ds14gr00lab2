package node.runnables;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import node.Node;

public class TCPListener implements Closeable, Runnable {

	private boolean stopped;
	private int port;
	private String dir = null;
	private ServerSocket serverSocket = null;
	private Socket socket = null;
	private ExecutorService executor = null;
	private String componentName = null;
	private ThreadLocal<DateFormat> df = null;

	private int minRes = 0;
	private Node node;

	public TCPListener(int port, String componentName, String dir) {

		this.stopped = false;
		this.port = port;
		this.dir = dir;
		this.executor = Executors.newCachedThreadPool();
		this.componentName = componentName;
		this.df = new ThreadLocal<DateFormat>() {

			@Override
			protected SimpleDateFormat initialValue() {
				return new SimpleDateFormat("yyyyMMdd_HHmmss.SSS");
			}
		};
	}

	@Override
	public void run() {

		try {

			serverSocket = new ServerSocket(port);

			while (!stopped) {

				socket = serverSocket.accept();

				BufferedReader reader = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				String line = reader.readLine();

				if (line != null) {

					String[] parts = line.split(" ");

					if ("!compute".equals(parts[0]))
						executor.execute(new CommandExecutor(line, socket
								.getOutputStream(), componentName, df, dir,
								minRes, node));

					if ("!logs".equals(parts[0]))
						executor.execute(new LogExecutor(socket
								.getOutputStream(), componentName, dir));

				}
			}

		} catch (SocketException e) {

			// Socket closed
		} catch (IOException e) {

			System.err.println("Can not listen on port: " + port);
		}
	}

	@Override
	public void close() {

		stopped = true;
		boolean shutDown = false;

		executor.shutdown();

		try {

			shutDown = executor.awaitTermination(1, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {

		} finally {

			if (!shutDown)
				executor.shutdownNow();
		}

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

	// stage 1 function
	public void setMinRes(int mRes) {
		this.minRes = mRes;
	}

	public void setNode(Node node) {
		this.node = node;
	}
}
