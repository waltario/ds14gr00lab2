package controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import util.Config;
import cli.Command;
import cli.Shell;
import controller.helpers.StatsCollector;
import controller.helpers.User;
import controller.runnables.TCPListener;
import controller.runnables.UDPListener;

public class CloudController implements ICloudControllerCli, Runnable {

	private String componentName = null;
	private Config config = null;
	private InputStream userRequestStream = null;
	private PrintStream userResponseStream = null;
	private StatsCollector collector = null;
	private ExecutorService executor = null;
	private Shell shell = null;
	private TCPListener accepter = null;
	private UDPListener receiver = null;

	/**
	 * @param componentNamenThreads
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public CloudController(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {

		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;
		this.executor = Executors.newFixedThreadPool(3);
		this.collector = StatsCollector.getInstance();

		initConfig();
		initRunnables();
	}

	@Override
	public void run() {

		executor.execute(shell);
		executor.execute(accepter);
		executor.execute(receiver);
	}

	@Override
	@Command
	public String nodes() throws IOException {

		return formOutput(collector.getNodeMap());
	}

	@Override
	@Command
	public String users() throws IOException {

		return formOutput(collector.getUserMap());
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

		accepter.close();
		receiver.close();
		shell.close();

		return "Server turned off!";
	}

	private void initConfig() {

		String[] tmp = null;
		User user = null;

		Config userConf = new Config("user");
		List<String> unameList = new ArrayList<String>();

		for (String key : userConf.listKeys()) {

			tmp = key.split("\\.");

			if (tmp[1].equals("password"))
				unameList.add(tmp[0]);
		}

		for (String uname : unameList) {

			user = new User(uname, userConf.getString(uname + ".password"),
					userConf.getInt(uname + ".credits"));
			collector.addUser(user);
		}
	}

	private void initRunnables() {

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		accepter = new TCPListener(config.getInt("tcp.port"));
		receiver = new UDPListener(config.getInt("udp.port"),
				config.getInt("node.timeout"),
				config.getInt("node.checkPeriod"));
	}

	private String formOutput(ConcurrentHashMap<?, ?> map) {

		StringBuilder sb = new StringBuilder();

		Collection<?> list = map.values();

		for (Object o : list) {

			sb.append(o);
			sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link CloudController}
	 *            component
	 */
	public static void main(String[] args) {

		new CloudController(args[0], new Config("controller"), System.in,
				System.out).run();
	}

}
