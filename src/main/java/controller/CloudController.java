package controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import model.ComputationRequestInfo;
import util.Config;
import util.Keys;
import util.SecurityUtils;
import admin.INotificationCallback;
import cli.Command;
import cli.Shell;
import controller.helpers.NodeHelper;
import controller.helpers.StatsCollector;
import controller.helpers.User;
import controller.runnables.TCPListener;
import controller.runnables.UDPListener;

public class CloudController implements ICloudControllerCli, IAdminConsole,
		Runnable {

	private String componentName = null;
	private Config config = null;
	private InputStream userRequestStream = null;
	private PrintStream userResponseStream = null;
	private StatsCollector collector = null;
	private ExecutorService executor = null;
	private Shell shell = null;
	private TCPListener accepter = null;
	private UDPListener receiver = null;
	private Registry registry = null;
	private IAdminConsole adminConsole = null;

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

		SecurityUtils.registerBouncyCastle();

		initConfig();
		initRunnables();

		// get private key for controller and set
		try {
			collector.setPrivateKey(Keys.readPrivatePEM(new File(config
					.getString("key"))));
			collector.setPublicKeyPathClient(config.getString("keys.dir"));

		} catch (IOException e) {

		}
	}

	@Override
	public void run() {

		try {

			initRegistry();
		} catch (RuntimeException e) {

			System.err.println(e.getMessage());
		}

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
		receiver.setRes(config.getInt("controller.rmax"));
	}

	private void initRegistry() throws RuntimeException {

		try {
			registry = LocateRegistry.createRegistry(config
					.getInt("controller.rmi.port"));
			adminConsole = (IAdminConsole) UnicastRemoteObject.exportObject(
					this, 0);
			registry.bind(config.getString("binding.name"), adminConsole);
		} catch (RemoteException e) {

			throw new RuntimeException("Error while initializing Registry!");
		} catch (AlreadyBoundException e) {

			throw new RuntimeException("Error while binding remote object!");
		}
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

	@Override
	public boolean subscribe(String username, int credits,
			INotificationCallback callback) throws RemoteException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public List<ComputationRequestInfo> getLogs() throws RemoteException {

		List<NodeHelper> nodesList = collector.getOnlineNodes();

		for (NodeHelper node : nodesList) {

			try {

				Socket socket = new Socket(node.getAddress(), node.getPort());

				PrintStream writer = new PrintStream(socket.getOutputStream(),
						true);
				writer.println("!logs");
				writer.flush();
				ObjectInputStream ois = new ObjectInputStream(
						socket.getInputStream());

				@SuppressWarnings("unchecked")
				List<ComputationRequestInfo> list = (List<ComputationRequestInfo>) ois
						.readObject();

				if (writer != null)
					writer.close();

				if (ois != null)
					ois.close();

				if (socket != null)
					socket.close();

				return list;

			} catch (IOException e) {

			} catch (ClassNotFoundException e) {

				System.err.println("There is a failure!");
			}
		}

		return null;
	}

	@Override
	public LinkedHashMap<Character, Long> statistics() throws RemoteException {

		return sortHashMapByValues(collector.getStatisticsMap());
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

	private LinkedHashMap<Character, Long> sortHashMapByValues(
			HashMap<Character, Long> map) {

		List<Character> mapKeys = new ArrayList<Character>(map.keySet());
		List<Long> mapValues = new ArrayList<Long>(map.values());
		Collections.sort(mapValues, Collections.reverseOrder());
		Collections.sort(mapKeys, Collections.reverseOrder());

		LinkedHashMap<Character, Long> sortedMap = new LinkedHashMap<Character, Long>();

		Iterator<Long> valueIt = mapValues.iterator();

		while (valueIt.hasNext()) {

			Object val = valueIt.next();
			Iterator<Character> keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {

				Object key = keyIt.next();
				String comp1 = map.get(key).toString();
				String comp2 = val.toString();

				if (comp1.equals(comp2)) {

					map.remove(key);
					mapKeys.remove(key);
					sortedMap.put((Character) key, (Long) val);
					break;
				}
			}
		}
		return sortedMap;
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
