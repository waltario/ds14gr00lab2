package node;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import node.runnables.PacketSender;
import node.runnables.TCPListener;
import util.Config;
import cli.Command;
import cli.Shell;

public class Node implements INodeCli, Runnable {

	private String componentName = null;
	private Config config = null;
	private InputStream userRequestStream = null;
	private PrintStream userResponseStream = null;
	private String dir = null;
	private String operators = null;
	private String host = null;
	private int udpPort;
	private int tcpPort;
	private int alivePeriod;
	private ExecutorService executor = null;
	private Shell shell = null;
	private PacketSender packetSender = null;
	private TCPListener tcpListener = null;

	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Node(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		executor = Executors.newFixedThreadPool(3);

		initConfig();
		initRunnables();
	}

	private void initConfig() {

		dir = config.getString("log.dir");
		operators = config.getString("node.operators");
		host = config.getString("controller.host");
		udpPort = config.getInt("controller.udp.port");
		tcpPort = config.getInt("tcp.port");
		alivePeriod = config.getInt("node.alive");
	}

	private void initRunnables() {

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		packetSender = new PacketSender(host, udpPort, tcpPort, alivePeriod,
				operators);
		tcpListener = new TCPListener(tcpPort, componentName, dir);
	}

	@Override
	public void run() {

		executor.execute(shell);
		executor.execute(packetSender);
		executor.execute(tcpListener);
	}

	@Override
	@Command
	public String exit() throws IOException {

		boolean shutDown = false;

		executor.shutdown();

		try {

			shutDown = executor.awaitTermination(1, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {

		} finally {

			if (!shutDown)
				executor.shutdownNow();
		}

		if (packetSender != null)
			packetSender.close();

		if (tcpListener != null)
			tcpListener.close();

		if (shell != null)
			shell.close();

		return "Node turned off!";
	}

	@Override
	public String history(int numberOfRequests) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Node} component,
	 *            which also represents the name of the configuration
	 */
	public static void main(String[] args) {

		new Node(args[0], new Config(args[0]), System.in, System.out).run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String resources() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
