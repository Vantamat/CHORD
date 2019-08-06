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

public class Node {
	
	private String nodeIP;
	private BigInteger nodeID;
	private int m;
	private BigInteger ringDimension;
	
	private Node predecessor;
	private Node successor;
	
	private LinkedHashMap<BigInteger, Node> fingerTable = new LinkedHashMap<BigInteger, Node>();
	
	private ServerSocket serverSocket;
	private Listener listener;

	public Node() throws NoSuchAlgorithmException, IOException{
		
		nodeIP = InetAddress.getLocalHost().getHostAddress();
		
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] hash = digest.digest(nodeIP.getBytes(StandardCharsets.UTF_8));

		nodeID = new BigInteger(1, hash);
		
		m = hash.length * 8;
		ringDimension = BigInteger.valueOf((long) 2).pow(m);
		
		for(int i = 0; i < m; i++) {
			BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(i)).mod(ringDimension);
			Node entryValue = null;
			fingerTable.put(entryKey, entryValue);
		}		
		System.out.println("nodeIP\t" + nodeIP + "\nm\t" + m + "\nnodeID\t" + nodeID + "\n" + digest.toString());
	
		/*
		for (BigInteger key: fingerTable.keySet()) {
            System.out.println(key + "\t" + fingerTable.get(key));
		}
		*/
		
		serverSocket = new ServerSocket(2345);
		listener = new Listener(serverSocket);
		new Thread(listener).start();
	}
	
	public Node findSuccessor(BigInteger nodeID) {
		if(this.nodeID.compareTo(this.successor.getNodeID()) == 1)
			if((nodeID.compareTo(this.nodeID) == 1 && nodeID.compareTo(ringDimension) == -1) || (nodeID.compareTo(BigInteger.valueOf((long) 0)) != -1 && nodeID.compareTo(this.successor.getNodeID()) != 1))
				return this.successor;
		if(nodeID.compareTo(this.nodeID) == 1 && nodeID.compareTo(this.successor.getNodeID()) != 1)
			return this.successor;
		return closestPrecedingNode(nodeID).findSuccessor(nodeID);
	}
	
	private Node closestPrecedingNode(BigInteger nodeID) { //gestire i casi con fingerTable.get(i).getNodeID() == NULL
		for(int i = m; i > 0; i--) {
			if(this.nodeID.compareTo(this.successor.getNodeID()) == 1)
				if((fingerTable.get(i).getNodeID().compareTo(this.nodeID) == 1 && fingerTable.get(i).getNodeID().compareTo(ringDimension) == -1) || (fingerTable.get(i).getNodeID().compareTo(BigInteger.valueOf((long) 0)) != -1 && fingerTable.get(i).getNodeID().compareTo(this.successor.getNodeID()) != 1))
					return fingerTable.get(i);
			if(fingerTable.get(i).getNodeID().compareTo(this.nodeID) == 1 && fingerTable.get(i).getNodeID().compareTo(nodeID) == -1)
				return fingerTable.get(i);
		}
		return this;
	}	 
	
	public void create() {
		this.predecessor = this; //con NULL crasha al primo stabilize
		this.successor = this;
	}	
	
	public void join(Node n) {
		this.predecessor = null;
		this.successor = n.findSuccessor(this.nodeID);
	}
	
	public void leave() throws IOException {
		serverSocket.close();
	}
	
	public void stabilize() {
		Node node = successor.getPredecessor().getSuccessor();
		if(this.nodeID.compareTo(this.successor.getNodeID()) == 1)
			if((node.getNodeID().compareTo(this.nodeID) == 1 && node.getNodeID().compareTo(ringDimension) == -1) || (node.getNodeID().compareTo(BigInteger.valueOf((long) 0)) != -1 && node.getNodeID().compareTo(this.successor.getNodeID()) == -1))
				this.successor = node;
		if(node.getNodeID().compareTo(this.nodeID) == 1 && node.getNodeID().compareTo(this.successor.getNodeID()) == -1)
			this.successor = node;
		this.successor.notify(this);
		
	}

	public void notify(Node node) {
		if(this.predecessor != null && this.predecessor.getNodeID().compareTo(this.nodeID) == 1)
			if((node.getNodeID().compareTo(this.predecessor.getNodeID()) == 1 && node.getNodeID().compareTo(ringDimension) == -1) || (node.getNodeID().compareTo(BigInteger.valueOf((long) 0)) != -1 && node.getNodeID().compareTo(this.nodeID) == -1))
				this.predecessor = node;
		if(this.predecessor == null || (node.getNodeID().compareTo(this.predecessor.getNodeID()) == 1 && node.getNodeID().compareTo(this.nodeID) == -1))
			this.predecessor = node;
	}
	
	public void fixFingers() {
		
	}
	
	public void checkPredecessor() throws UnknownHostException, IOException {
		if(InetAddress.getByName(this.predecessor.getNodeIP()).isReachable(2000))
			this.predecessor = null;
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

	public Node getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(Node predecessor) {
		this.predecessor = predecessor;
	}

	public Node getSuccessor() {
		return successor;
	}

	public void setSuccessor(Node successor) {
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