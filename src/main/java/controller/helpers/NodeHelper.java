package controller.helpers;

import java.net.InetAddress;

public class NodeHelper implements Comparable<NodeHelper> {

	private InetAddress addr = null;
	private int port;
	private boolean online;
	private long usage;
	private char[] operators;
	private long updateTime;

	public NodeHelper(InetAddress addr, int port, String operators) {

		this.addr = addr;
		this.port = port;
		this.online = true;
		this.usage = 0;
		this.operators = operators.toCharArray();
		updateTime = System.currentTimeMillis();
	}

	public InetAddress getAddress(){
		return addr;
	}
	
	public int getPort() {
		return port;
	}
	
	public long addUsage(long usage) {

		this.usage += usage;
		return this.usage;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
		this.updateTime = System.currentTimeMillis();
	}

	public long getUsage() {
		return usage;
	}

	public char[] getOperators() {
		return operators;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();

		sb.append("IP: " + addr.getHostAddress());
		sb.append(" ");
		sb.append("Port: " + port);
		sb.append(" ");
		sb.append(isOnline() ? "online" : "offline");
		sb.append(" ");
		sb.append("Usage: " + usage);

		return sb.toString();
	}

	@Override
	public int compareTo(NodeHelper other) {

		if (this.usage < other.getUsage())
			return -1;

		if (this.usage > other.getUsage())
			return 1;

		return 0;
	}
}
