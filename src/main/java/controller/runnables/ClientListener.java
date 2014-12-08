package controller.runnables;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import controller.helpers.NodeHelper;
import controller.helpers.StatsCollector;

public class ClientListener implements Runnable, Closeable {

	private Socket socket = null;
	private BufferedReader reader = null;
	private PrintStream writer = null;
	private boolean stopped;
	private StatsCollector collector = null;
	private String uname = null;

	public ClientListener(Socket socket) throws IOException {

		this.socket = socket;
		this.stopped = false;
		collector = StatsCollector.getInstance();

		initIO();
	}

	private void initIO() throws IOException {

		reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		writer = new PrintStream(socket.getOutputStream(), true);
	}

	@Override
	public void run() {

		String input = null;
		int exitCount = 0;

		while (!stopped) {

			try {

				input = reader.readLine();

				if (input == null)
					break;

				writer.println(executeCommand(input));

			} catch (IOException e) {

				System.err.println("Error while reading from InputStream!");
				exitCount++;
				if (exitCount == 3)
					stopped = true;
			}
		}
	}

	@Override
	public void close() {

		stopped = true;

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
	}

	private String executeCommand(String input) {

		String[] command = input.split(" ");

		if (command[0].equals("!exit"))
			return "Exitting client!";

		if (!command[0].equals("!login")
				&& (uname == null || !collector.isLoggedIn(uname)))
			return "You have to login first!";

		switch (command[0]) {

		case "!login":
			return loginCommand(command[1], command[2]);
		case "!credits":
			return "You have " + collector.getCredits(uname) + " credits left.";
		case "!buy":
			return buyCommand(command[1]);
		case "!list":
			return collector.listOperators();
		case "!compute":
			return computeCommand(command);
		case "!logout":
			collector.logout(uname);
			this.uname = null;
			return "Succesfully logged out!";
		}

		return "CLIENT SAID: " + input;
	}

	private String loginCommand(String uname, String pass) {

		boolean loggedIn = collector.isLoggedIn(uname);

		if (!loggedIn) {

			boolean match = collector.login(uname, pass);

			if (match) {

				this.uname = uname;
				return "Successfully logged in.";
			}

			return "Wrong username or password.";
		}

		if (this.uname == null)
			this.uname = uname;

		return "You are already logged in!";
	}

	private String buyCommand(String arg) {

		try {

			long credits = Long.valueOf(arg);
			return "You now have " + collector.buyCredits(uname, credits)
					+ " credits.";
		} catch (NumberFormatException e) {

			return "Not well formed command!";
		}
	}

	private String computeCommand(String[] comp) {

		int first;
		int second;
		Character op;
		String rsltTmp = null;

		if (comp.length % 2 != 0)
			return "Wrong number of operations!";

		if (!collector.canCompute(uname, (comp.length / 2) - 1))
			return "Not enough credits!";

		try {

			first = Integer.valueOf(comp[1]);
			int i = 2;

			while (i < comp.length) {

				op = comp[i++].charAt(0);
				second = Integer.valueOf(comp[i++]);

				rsltTmp = compute(first, second, op);

				try {

					first = Integer.valueOf(rsltTmp);

				} catch (NumberFormatException e) {

					return rsltTmp;
				}
			}

		} catch (NumberFormatException e) {

			return "Not well formed operations!";
		} catch (IOException e) {

			return "Node not reachable!";
		}

		return String.valueOf(first);
	}

	private String compute(int first, int second, char op) throws IOException {

		String result = null;

		NodeHelper node = collector.getLowestNodeFor(op);

		if (node == null)
			return "Not supported operation!";

		Socket socket = new Socket(node.getAddress(), node.getPort());
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				socket.getInputStream()));
		PrintStream writer = new PrintStream(socket.getOutputStream(), true);

		writer.println("!compute " + first + " " + op + " " + second);
		result = reader.readLine();

		if (writer != null)
			writer.close();

		if (reader != null)
			reader.close();

		if (socket != null)
			socket.close();

		collector.arrangeCredits(node.getPort(), uname, result);

		return result;
	}
}
