package node;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import org.json.simple.JSONObject;

public class Node {
	
	/* 
	 * Args:
	 *		nodeIP:
	 *		nodeID:
	 *		m:
	 *		hash:
	 *		ringDimension:
	 *		digest:
	 *		address:
	 *		predecessor:
	 *		successor:
	 *		fingerTable:
	 *		serverSocket:
	 *		Listener:
	*/
	private String nodeIP;
	private BigInteger nodeID;
	private int m;
	private byte[] hash;
	private BigInteger ringDimension;
	
	private MessageDigest digest;
	
	private InetAddress address;
	private InetAddress predecessor;
	private InetAddress successor;
	
	private LinkedHashMap<BigInteger, InetAddress> fingerTable = new LinkedHashMap<BigInteger, InetAddress>();
	
	private ServerSocket serverSocket;
	private Listener listener;

	public Node() throws IOException, NoSuchAlgorithmException{
		
		this.address = InetAddress.getLocalHost();
		this.nodeIP = this.address.getHostAddress();
		this.nodeID = evaluateID(this.nodeIP);
		this.m = hash.length * 8;
		this.ringDimension = BigInteger.valueOf((long) 2).pow(m);
		
		for(int i = 0; i < m; i++) {
			BigInteger entryKey = this.nodeID.add(BigInteger.valueOf((long) 2).pow(i)).mod(ringDimension);
			InetAddress entryValue = null;
			this.fingerTable.put(entryKey, entryValue);
		}		
		System.out.println("nodeIP\t" + this.nodeIP + "\nm\t" + this.m + "\nnodeID\t" + this.nodeID + "\n" + this.digest.toString());
	
		/*
		for (BigInteger key: fingerTable.keySet()) {
            System.out.println(key + "\t" + fingerTable.get(key));
		}
		*/
		
		this.serverSocket = new ServerSocket(2345);
		this.listener = new Listener(serverSocket);
		new Thread(this.listener).start();

		printNodeInformation();
		
	}
	
	public InetAddress findSuccessor(InetAddress node) {
		if(this.nodeID.compareTo(evaluateID(this.successor.getHostAddress())) == 1)
			if((evaluateID(node.getHostAddress()).compareTo(this.nodeID) == 1
						&& evaluateID(node.getHostAddress()).compareTo(ringDimension) == -1)
					|| (evaluateID(node.getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
						&& evaluateID(node.getHostAddress()).compareTo(evaluateID(this.successor.getHostAddress())) != 1))
				return this.successor;
		if(evaluateID(node.getHostAddress()).compareTo(this.nodeID) == 1
				&& evaluateID(node.getHostAddress()).compareTo(evaluateID(this.successor.getHostAddress())) != 1)
			return this.successor;
		return closestPrecedingNode(node).findSuccessor(node);
	}
	
	private InetAddress closestPrecedingNode(InetAddress node) throws UnknownHostException, NoSuchAlgorithmException { //gestire i casi con fingerTable.get(i).getNodeID() == NULL
		for(int i = m; i > 0; i--) {
			if(this.nodeID.compareTo(evaluateID(this.successor.getHostAddress())) == 1)
				if((evaluateID(fingerTable.get(i).getHostAddress()).compareTo(this.nodeID) == 1
							&& evaluateID( fingerTable.get(i).getHostAddress()).compareTo(ringDimension) == -1)
						|| (evaluateID(fingerTable.get(i).getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
							&& evaluateID(fingerTable.get(i).getHostAddress()).compareTo(evaluateID(this.successor.getHostAddress())) != 1))
					return fingerTable.get(i);
			if(fingerTable.get(i) != null 
					&& evaluateID(fingerTable.get(i).getHostAddress()).compareTo(this.nodeID) == 1
					&& evaluateID(fingerTable.get(i).getHostAddress()).compareTo(nodeID) == -1)
				return fingerTable.get(i);
		}
		return this.address;
	}	 
	
	public void create() throws UnknownHostException {
		this.predecessor = this.address; //con NULL crasha al primo stabilize
		this.successor = this.address;
	}	
	
	public void join(InetAddress node) {
		this.predecessor = null;
		JSONObject json = new JSONObject();
		json.put("op_code", Command.JOIN);
		json.put("ip", nodeIP);
	}
	
	public void leave() throws IOException {
		serverSocket.close();
	}
	
	public void stabilize() {
		InetAddress node = successor.getPredecessor().getSuccessor();
		if(this.nodeID.compareTo(evaluateID(this.successor.getHostAddress())) == 1)
			if((evaluateID(node.getHostAddress()).compareTo(this.nodeID) == 1
						&& evaluateID(node.getHostAddress()).compareTo(ringDimension) == -1)
					|| (evaluateID(node.getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
						&& evaluateID(node.getHostAddress()).compareTo(evaluateID(this.successor.getHostAddress())) == -1))
				this.successor = node;
		if(evaluateID(node.getHostAddress()).compareTo(this.nodeID) == 1 && evaluateID(node.getHostAddress()).compareTo(evaluateID(this.successor.getHostAddress())) == -1)
			this.successor = node;
		this.successor.notify(this.address);		
	}

	public void notify(InetAddress node) throws NoSuchAlgorithmException {
		if(this.predecessor != null && evaluateID(this.predecessor.getHostAddress()).compareTo(this.nodeID) == 1)
			if((evaluateID(node.getHostAddress()).compareTo(evaluateID(this.successor.getHostAddress())) == 1
						&& evaluateID(node.getHostAddress()).compareTo(ringDimension) == -1)
					|| (evaluateID(node.getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
						&& evaluateID(node.getHostAddress()).compareTo(this.nodeID) == -1))
				this.predecessor = node;
		if(this.predecessor == null
					|| (evaluateID(node.getHostAddress()).compareTo(evaluateID(this.predecessor.getHostAddress())) == 1 && evaluateID(node.getHostAddress()).compareTo(this.nodeID) == -1))
			this.predecessor = node;
	}
	
	public void fixFingers() {
		
	}
	
	public void checkPredecessor() throws UnknownHostException, IOException {
		if(InetAddress.getByName(this.predecessor.getHostAddress()).isReachable(2000))
			this.predecessor = null;
	}
	
	private BigInteger evaluateID(String nodeIP) throws NoSuchAlgorithmException {
		digest = MessageDigest.getInstance("SHA-1");
		byte[] hash = digest.digest(nodeIP.getBytes(StandardCharsets.UTF_8));
		
		return new BigInteger(1, hash);
	}
	
	private void printNodeInformation() throws NoSuchAlgorithmException {
		System.out.println("Node IP\t\t" + this.nodeIP + "\nID dimension\t" + this.m + "\nNode ID\t\t" + this.nodeID + "\n" + this.digest.toString());
		if(this.successor != null)
			System.out.println("Successor ID\t\t" + evaluateID(this.successor.getHostAddress()));
		if(this.predecessor != null)
			System.out.println("Predecessor ID\t\t" + evaluateID(this.predecessor.getHostAddress()));
		/*
		for (BigInteger key: fingerTable.keySet()) {
            System.out.println(key + "\t" + fingerTable.get(key));
		}
		*/
	}
	
	public String getNodeIP() {
		return nodeIP;
	}

	public void setNodeIP(String nodeIP) {
		this.nodeIP = nodeIP;
	}

	public BigInteger getNodeID() {
		return nodeID;
	}

	public void setNodeID(BigInteger nodeID) {
		this.nodeID = nodeID;
	}

	public InetAddress getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(InetAddress predecessor) {
		this.predecessor = predecessor;
	}

	public InetAddress getSuccessor() {
		return successor;
	}

	public void setSuccessor(InetAddress successor) {
		this.successor = successor;
	}

	public static void main (String[] args) throws NoSuchAlgorithmException, IOException {
		Node n1 = new Node();
		/*Node n2 = new Node();
		n1.create();
		n2.join(n1);
		n2.setNodeID(n2.getNodeID().add(BigInteger.valueOf((long) 1)));
		System.out.println("New N2 ID: " + n2.getNodeID());
		n1.stabilize();
		n2.stabilize();
		System.out.println("N1 ID: " + n1.getNodeID() + "\nN1 successor: " + n1.successor.getNodeID() + "\nN1 predecessor: " + n1.predecessor.getNodeID());
		System.out.println("N2 ID: " + n2.getNodeID() + "\nN2 successor: " + n2.successor.getNodeID() + "\nN2 predecessor: " + n2.predecessor.getNodeID());	
		n1.stabilize();
		//n2.join(n1);
		System.out.println(n1.successor);*/
	}
}