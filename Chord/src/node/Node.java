package node;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
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
	
	private BigInteger predecessor;
	private BigInteger successor;
	
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
	
		for (BigInteger name: fingerTable.keySet()) {
            System.out.println(name);
		}
		
		serverSocket = new ServerSocket(2345);
		listener = new Listener(serverSocket);
		new Thread(listener).start();
	}
	
	private BigInteger findSuccessor(BigInteger ID) {
		if(ID.compareTo(this.nodeID) == 1 && ID.compareTo(this.successor) != 1)
			return this.successor;
		else
			return closestPrecedingNode(ID).findSuccessor(ID);
	}
	
	private Node closestPrecedingNode(BigInteger ID) {
		for(int i = m; i > 0; i--) {
			if(fingerTable.get(i).nodeID.compareTo(this.nodeID) == 1 && fingerTable.get(i).nodeID.compareTo(ID) == -1)
				return fingerTable.get(i);
		}
		return this;
	}	 
	
	public void create() {
		predecessor = null;
		successor = this.nodeID;
	}	
	
	public void join(Node n) {
		predecessor = null;
		successor = n.findSuccessor(this.nodeID);
	}
	
	public void leave() throws IOException {
		serverSocket.close();
	}
	
	public static void main (String[] args) throws NoSuchAlgorithmException, IOException {
		Node n1 = new Node();
		//Node n2 = new Node();
		n1.create();
		//n2.join(n1);
		System.out.println(n1.successor);
	}
}