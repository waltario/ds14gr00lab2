package node.runnables;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import model.ComputationRequestInfo;

public class LogExecutor implements Runnable, Closeable {

	private ObjectOutputStream oos = null;
	private String componentName = null;
	private String dir = null;

	public LogExecutor(OutputStream os, String componentName,
			String dir) throws IOException {

		this.oos = new ObjectOutputStream(os);
		this.componentName = componentName;
		this.dir = dir;
	}

	@Override
	public void run() {

		List<ComputationRequestInfo> logList = new ArrayList<ComputationRequestInfo>();

		final File folder = new File(dir);

		for (final File fileEntry : folder.listFiles()) {

			String fileName = fileEntry.getName();
			String filePath = ".\\" + fileEntry.getPath();

			String[] parts = fileName.split("_");
			fileName = parts[0] + "_" + parts[1];
			
			List<String> contents = new ArrayList<String>();

			BufferedReader reader = null;
			String line = null;

			try {

				reader = new BufferedReader(new FileReader("./" + filePath));

				while ((line = reader.readLine()) != null) {

					contents.add(line);
				}
			} catch (IOException e) {

				System.err.println("Error while reading from File!");
			} finally {
				try {
					if (reader != null)
						reader.close();
				} catch (IOException ex) {
				}
			}

			ComputationRequestInfo tmp = new ComputationRequestInfo();
			tmp.setTimestamp(fileName);
			tmp.setNodeName(componentName);

			if (contents.get(0) != null)
				tmp.setComputation(contents.get(0));

			if (contents.get(1) != null)
				tmp.setResult(contents.get(1));

			logList.add(tmp);
		}

		try {

			oos.writeObject(logList);
		} catch (IOException e) {

			System.err.println("Error while sending log list!");
			e.printStackTrace(System.out);
		}

		close();
	}

	@Override
	public void close() {

		if (oos != null)
			try {

				oos.close();
			} catch (IOException e) {
			}
	}
}
