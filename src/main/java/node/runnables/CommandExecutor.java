package node.runnables;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.Date;

public class CommandExecutor implements Runnable, Closeable {

	private BufferedReader reader = null;
	private PrintStream writer = null;
	private String componentName = null;
	private ThreadLocal<DateFormat> df = null;
	private String dir = null;

	CommandExecutor(InputStream is, OutputStream os, String componentName,
			ThreadLocal<DateFormat> df, String dir) throws IOException {

		this.reader = new BufferedReader(new InputStreamReader(is));
		this.writer = new PrintStream(os);
		this.componentName = componentName;
		this.df = df;
		this.dir = dir;
	}

	@Override
	public void run() {

		try {

			new File("./" + dir).mkdirs();
			String input = null;

			while ((input = reader.readLine()) != null) {

				writer.println(compute(input));
			}

		} catch (IOException e) {
		}

		close();
	}

	private String compute(String expression) {

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

		if (reader != null)
			try {
				reader.close();
			} catch (IOException e) {

			}
	}

}
