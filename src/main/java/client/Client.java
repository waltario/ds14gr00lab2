package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import util.Config;
import cli.Command;
import cli.Shell;

public class Client implements IClientCli, Runnable {

	private String componentName = null;
	private Config config = null;
	private InputStream userRequestStream = null;
	private PrintStream userResponseStream = null;
	private Shell shell = null;
	private ExecutorService executor = null;
	private Socket socket = null;
	private BufferedReader reader = null;
	private PrintStream writer = null;

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
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		initRunnables();
	}

	private void initRunnables() {

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);
		executor = Executors.newFixedThreadPool(1);
	}

	@Override
	public void run() {

		try {

			socket = new Socket(config.getString("controller.host"),
					config.getInt("controller.tcp.port"));
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			writer = new PrintStream(socket.getOutputStream(), true);
			executor.execute(shell);

		} catch (IOException e) {

			System.err.println("Can not connect to server at "
					+ config.getString("controller.host") + ":"
					+ config.getInt("controller.tcp.port"));
		}
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {

		writer.println("!login " + username + " " + password);
		return reader.readLine();
	}

	@Override
	@Command
	public String logout() throws IOException {

		writer.println("!logout");
		return reader.readLine();
	}

	@Override
	@Command
	public String credits() throws IOException {

		writer.println("!credits");
		return reader.readLine();
	}

	@Override
	@Command
	public String buy(long credits) throws IOException {

		writer.println("!buy " + credits);
		return reader.readLine();
	}

	@Override
	@Command
	public String list() throws IOException {

		writer.println("!list");
		return reader.readLine();
	}

	@Override
	@Command
	public String compute(String term) throws IOException {

		writer.println("!compute " + term);
		return reader.readLine();
	}

	@Override
	@Command
	public String exit() throws IOException {

		writer.println("!exit");
		String ret = reader.readLine();

		close();

		return ret;
	}

	private void close() {

		boolean isShutDown = false;

		executor.shutdown();

		try {

			isShutDown = executor.awaitTermination(1, TimeUnit.MILLISECONDS);

		} catch (InterruptedException e) {

		} finally {

			if (!isShutDown)
				executor.shutdownNow();
		}

		if (writer != null)
			writer.close();

		if (reader != null)
			try {
				reader.close();
			} catch (IOException e) {

			}

		if (socket != null)
			try {
				socket.close();
			} catch (IOException e) {

			}

		shell.close();
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) {

		new Client(args[0], new Config("client"), System.in, System.out).run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	@Command
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
