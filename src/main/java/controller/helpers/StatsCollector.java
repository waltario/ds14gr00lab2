package controller.helpers;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class StatsCollector {

	private static StatsCollector instance = null;
	private ConcurrentHashMap<Integer, NodeHelper> nodeMap = null;
	private ConcurrentHashMap<Character, List<Integer>> operatorMap = null;
	private ConcurrentHashMap<String, User> userMap = null;
	
	private PrivateKey privateKeyController = null;
	private String publicKeyPathClient = null;

	private StatsCollector() {

		this.nodeMap = new ConcurrentHashMap<Integer, NodeHelper>();
		this.operatorMap = new ConcurrentHashMap<Character, List<Integer>>();
		this.userMap = new ConcurrentHashMap<String, User>();
	}

	public static StatsCollector getInstance() {

		if (instance == null)
			instance = new StatsCollector();

		return instance;
	}

	// ///////////////////////////////////////////////////
	// ///////////////////// NODE ////////////////////////
	// ///////////////////////////////////////////////////

	public boolean existsNode(Integer port) {

		return nodeMap.containsKey(port);
	}

	public void addNode(NodeHelper nodeHelper) {

		nodeMap.put(nodeHelper.getPort(), nodeHelper);
		char[] ops = nodeHelper.getOperators();

		for (int i = 0; i < ops.length; i++) {

			if (operatorMap.containsKey(ops[i])) {

				addToOperatorMap(ops[i], nodeHelper.getPort());
			} else {

				List<Integer> list = new ArrayList<Integer>();
				operatorMap.put(ops[i], list);
				addToOperatorMap(ops[i], nodeHelper.getPort());
			}
		}
	}

	private void addToOperatorMap(Character c, Integer port) {

		boolean contains = false;
		List<Integer> list = operatorMap.get(c);

		for (Integer p : list) {

			if (p == port) {
				contains = true;
				break;
			}
		}

		if (!contains) {

			operatorMap.get(c).add(port);
		}
	}

	public ConcurrentHashMap<Integer, NodeHelper> getNodeMap() {

		return nodeMap;
	}

	public void updateNode(Integer port, boolean status) {

		nodeMap.get(port).setOnline(status);
	}

	public String listOperators() {
				
		String ret = "";

		for (Character c : operatorMap.keySet()) {

			for (Integer i : operatorMap.get(c)) {

				if (nodeMap.get(i).isOnline()) {

					ret += c;
					break;
				}
			}
		}

		return ret;
	}

	public NodeHelper getLowestNodeFor(Character c) {

		List<Integer> portList = operatorMap.get(c);
		List<NodeHelper> nodes = new ArrayList<NodeHelper>();
		NodeHelper tmp = null;

		if (portList == null)
			return null;

		for (Integer port : portList) {

			tmp = nodeMap.get(port);

			if (tmp != null && tmp.isOnline())
				nodes.add(tmp);
		}

		if (nodes.size() <= 0)
			return null;

		Collections.sort(nodes);

		return nodes.get(0);
	}

	public void arrangeCredits(Integer port, String uname, String result) {

		nodeMap.get(port).addUsage(result.length() * 50);
		userMap.get(uname).removeCredits(50);
	}

	// ///////////////////////////////////////////////////
	// ///////////////////// USER ////////////////////////
	// ///////////////////////////////////////////////////

	public void addUser(User user) {

		this.userMap.put(user.getUname(), user);
	}

	public ConcurrentHashMap<String, User> getUserMap() {

		return userMap;
	}

	public boolean isLoggedIn(String uname) {
		
		User user = userMap.get(uname);

		if (user != null)
			return user.isOnline();

		return false;
	}

	public boolean login(String uname, String pass) {

		if (userMap.containsKey(uname)) {

			User user = userMap.get(uname);

			if (user.authenticate(pass)) {

				user.setOnline(true);
				return true;
			}
		}

		return false;
	}

	public boolean setOnline(String uname) {

		if (userMap.containsKey(uname)) {

			User user = userMap.get(uname);
			user.setOnline(true);
			return true;
			
		}

		return false;
	}
	
	public void logout(String uname) {

		userMap.get(uname).setOnline(false);
	}

	public long getCredits(String uname) {

		return userMap.get(uname).getCredits();
	}

	public long buyCredits(String uname, long credits) {

		return userMap.get(uname).buyCredits(credits);
	}

	public boolean canCompute(String uname, int opCount) {

		if (userMap.get(uname).getCredits() >= (opCount * 50))
			return true;

		return false;
	}
	
	// ///////////////////////////////////////////////////
	// ///////////////////// CONTROLLER KEYS ////////////////////////
	// ///////////////////////////////////////////////////

	public void setPrivateKey(PrivateKey pk){
		this.privateKeyController = pk;
	}
	
	public PrivateKey getPrivateKey(){
		return privateKeyController;
	}

	public String getPublicKeyPathClient() {
		return publicKeyPathClient;
	}

	public void setPublicKeyPathClient(String publicKeyPathClient) {
		this.publicKeyPathClient = publicKeyPathClient;
	}
	
}
