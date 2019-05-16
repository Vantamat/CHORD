package node;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
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

	public Node() throws UnknownHostException, NoSuchAlgorithmException{
		
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
	
		for (BigInteger key: fingerTable.keySet()) {
            System.out.println(key + "\t" + fingerTable.get(key));
		}
	}
	
	private Node findSuccessor(BigInteger nodeID) {
		if(nodeID.compareTo(this.nodeID) == 1 && nodeID.compareTo(this.successor.getNodeID()) != 1)
			return this.successor;
		else
			return closestPrecedingNode(nodeID).findSuccessor(nodeID);
	}
	
	private Node closestPrecedingNode(BigInteger nodeID) {
		for(int i = m; i > 0; i--) {
			if(fingerTable.get(i).nodeID.compareTo(this.nodeID) == 1 && fingerTable.get(i).nodeID.compareTo(nodeID) == -1)
				return fingerTable.get(i);
		}
		return this;
	}	 
	
	public void create() {
		this.predecessor = null;
		this.successor = this;
	}	
	
	public void join(Node n) {
		this.predecessor = null;
		this.successor = n.findSuccessor(this.nodeID);
	}
	
	public void stabilize() {
		Node node = successor.getPredecessor().getSuccessor();
		if(node.getNodeID().compareTo(this.nodeID) == 1 && node.getNodeID().compareTo(this.nodeID) == -1)
			this.successor = node;
		this.successor.notify(this);
		
	}

	public void notify(Node node) {
		if(this.predecessor == null || (node.getNodeID().compareTo(this.predecessor.getNodeID()) == 1 && node.getNodeID().compareTo(this.nodeID) == -1))
			this.predecessor = node;
	}
	
	public void fixFingers() {
		
	}
	
	public void checkPredecessor() {
		if(false/*TO ADD predecessor has failed*/)
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
		//Node n2 = new Node();
		n1.create();
		//Listener l = new Listener();//InetAddress.getLocalHost()
		//new Thread(l).start();
		//n2.join(n1);
		System.out.println(n1.successor.getNodeID());
	}
}