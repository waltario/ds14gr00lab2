package model;

import java.io.Serializable;

/**
 * Please note that this class is not needed for Lab 1, but will later be used
 * in Lab 2. Hence, you do not have to implement it for the first submission.
 */
public class ComputationRequestInfo implements Serializable,
		Comparable<ComputationRequestInfo> {

	private static final long serialVersionUID = 6036411780976517945L;
	private String timestamp = null;
	private String nodeName = null;
	private String computation = null;
	private String result = null;

	public ComputationRequestInfo() {

	}

	public ComputationRequestInfo(String timestamp, String nodeName,
			String computation, String ressult) {

		this.timestamp = timestamp;
		this.nodeName = nodeName;
		this.computation = computation;
		this.result = ressult;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public String getComputation() {
		return computation;
	}

	public void setComputation(String computation) {
		this.computation = computation;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	@Override
	public int compareTo(ComputationRequestInfo o) {

		return this.timestamp.compareTo(o.getTimestamp());
	}

	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append(timestamp);
		sb.append(" ");
		sb.append("[" + nodeName + "]:");
		sb.append(" ");
		sb.append(computation);
		sb.append(" = ");
		sb.append(result);

		return sb.toString();
	}
}
