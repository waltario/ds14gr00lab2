package controller.runnables;

import java.io.Closeable;
import java.util.Collection;

import controller.helpers.NodeHelper;
import controller.helpers.StatsCollector;

public class NodeChecker extends Thread implements Closeable {

	private int timeout;
	private int checkPeriod;
	private boolean stopped;
	private StatsCollector collector = null;

	public NodeChecker(int timeout, int checkPeriod) {

		super();

		this.timeout = timeout;
		this.checkPeriod = checkPeriod;
		this.stopped = false;
		collector = StatsCollector.getInstance();
	}

	@Override
	public void run() {

		Collection<NodeHelper> nodes = null;

		while (!stopped) {

			try {
				nodes = collector.getNodeMap().values();

				for (NodeHelper node : nodes) {

					if ((System.currentTimeMillis() - node.getUpdateTime()) >= timeout)
						synchronized (collector) {

							collector.updateNode(node.getPort(), false);
						}
				}

				sleep(checkPeriod);
			} catch (InterruptedException e) {

				// Closing Thread
			}
		}
	}

	@Override
	public void close() {

		stopped = true;
		this.interrupt();
	}

}
