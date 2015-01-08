package node.runnables;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.Date;

import node.Node;

public class CommandExecutor implements Runnable, Closeable {

	private String input = null;
	private PrintStream writer = null;
	private String componentName = null;
	private ThreadLocal<DateFormat> df = null;
	private String dir = null;

	private int minRes;
	private Node node;

	CommandExecutor(String input, OutputStream os, String componentName,
			ThreadLocal<DateFormat> df, String dir, int minRes, Node node)
			throws IOException {

		this.input = input;
		this.writer = new PrintStream(os);
		this.componentName = componentName;
		this.df = df;
		this.dir = dir;
		this.minRes = minRes;
		this.node = node;
	}

	@Override
	public void run() {

		writer.println(compute(input));

		close();
	}

	private String compute(String expression) {

		new File("./" + dir).mkdirs();

		String[] parts = expression.split(" ");

		if (parts.length == 4 && "!compute".equals(parts[0])) {

			try {

				int first = Integer.valueOf(parts[1]);
				int second = Integer.valueOf(parts[3]);
				char op = parts[2].charAt(0);
				String result = null;

				switch (op) {
				case '+':
					result = String.valueOf(first + second);
					break;
				case '-':
					result = String.valueOf(first - second);
					break;
				case '*':
					result = String.valueOf(first * second);
					break;
				case '/':
					if (second != 0)
						result = String.valueOf(Math.round((float) first
								/ (float) second));
					else
						result = "Division by 0!";

					break;
				default:
					return "Not supported operator: " + op;
				}

				writeLogFile(expression, result);

				return result;

			} catch (NumberFormatException e) {

				return "Computationrequest has wrong format!";
			}
		}

		// Receive !share request from Initiator, change newRes from Node in
		// preparation for the change
		else if ("!share".equals(parts[0])) {
			int newRes = Integer.parseInt(parts[1]); // System.out.println("!share!");
			node.setNewRes(newRes);
			if (newRes < minRes) {
				// System.out.println("!share nok");
				return "!nok";
			} else {
				// System.out.println("!share ok");
				return "!ok";
			}
		}

		// Receive !commit request from Initiator, finalize the change from the
		// last !share request
		else if ("!commit".equals(parts[0])) {
			// System.out.println("commandexecutor !commit, nres " +
			// node.getNewRes());
			node.setRes(node.getNewRes());
		}

		return "Wrong computationrequest!";
	}

	private void writeLogFile(String expression, String result) {

		String name = df.get().format(new Date()) + "_" + componentName;
		Path file = Paths.get("./" + dir + "/" + name + ".log");

		try {

			byte[] deneme = (expression.substring(9) + "\n" + result)
					.getBytes();

			Files.write(file, deneme, StandardOpenOption.CREATE);

		} catch (IOException e) {

			System.err.println("Error while managing log file!");
		}
	}

	@Override
	public void close() {

		if (writer != null)
			writer.close();
	}

}
