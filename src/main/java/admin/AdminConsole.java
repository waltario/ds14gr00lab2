package admin;

import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Key;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import model.ComputationRequestInfo;
import util.Config;
import cli.Command;
import cli.Shell;
import controller.IAdminConsole;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class AdminConsole implements IAdminConsole, Runnable {

	private String componentName = null;
	private Config config = null;
	private InputStream userRequestStream = null;
	private PrintStream userResponseStream = null;
	private Shell shell = null;
	private ExecutorService executor = null;
	private Registry registry = null;
	private IAdminConsole adminConsole = null;

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
	public AdminConsole(String componentName, Config config,
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

			initRegistry();
		} catch (RuntimeException e) {

			System.err.println(e.getMessage());
		}

		executor.execute(shell);
	}

	private void initRegistry() throws RuntimeException {

		try {

			registry = LocateRegistry.getRegistry(
					config.getString("controller.host"),
					config.getInt("controller.rmi.port"));

			adminConsole = (IAdminConsole) registry.lookup(config
					.getString("binding.name"));
		} catch (RemoteException e) {

			throw new RuntimeException("Error while obtaining registry!");
		} catch (NotBoundException e) {

			throw new RuntimeException("Error while looking for remote object!");
		}
	}

	@Command("subscribe")
	public String shellSubscribe(String username, int credits) {

		return null;
	}

	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Command("getLogs")
	public String shellGetLogs() throws RemoteException {

		List<ComputationRequestInfo> list = getLogs();

		if (list != null)
			Collections.sort(list);

		StringBuilder sb = new StringBuilder();
		for (ComputationRequestInfo cri : list) {

			sb.append(cri.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {

		return adminConsole.getLogs();
	}

	@Command("statistics")
	public String shellStatistics() throws RemoteException {

		LinkedHashMap<Character, Long> map = statistics();

		StringBuilder sb = new StringBuilder();

		for (Character op : map.keySet()) {

			sb.append(op);
			sb.append(" ");
			sb.append(map.get(op));
			sb.append("\n");
		}

		return sb.toString();
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {

		return adminConsole.statistics();
	}

	@Override
	public Key getControllerPublicKey() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUserPublicKey(String username, byte[] key)
			throws RemoteException {
		// TODO Auto-generated method stub
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link AdminConsole}
	 *            component
	 */
	public static void main(String[] args) {

		new AdminConsole(args[0], new Config(args[0]), System.in, System.out)
				.run();
	}
}
