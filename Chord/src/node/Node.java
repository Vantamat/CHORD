package node;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.Timer;

import org.json.JSONObject;


public class Node {
	/* 
	 *		nodeIP: IP address of the node represented as a string
	 *		nodeID: node identifier into the ring
	 *		m: number of bits of the node identifier
	 *		hash: output of the hash function
	 *		ringDimension: max number of nodes in the ring
	 *		digest: implementation of the specified algorithm
	 *		address: IP address of the current node
	 *		predecessor: IP address of the node predecessor
	 *		successor: IP address of the node successor
	 *		fingerTable: routing table with m entries
	 *	 	port: node port represented as an integer, fixed to 6007
	 *		serverSocket:
	 *		listener:
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
	
	private InetAddress succPred;

	private LinkedHashMap<BigInteger, InetAddress> fingerTable = new LinkedHashMap<BigInteger, InetAddress>();

	private int port;
	private ServerSocket serverSocket;
	private Listener listener;

	public Node() throws IOException, NoSuchAlgorithmException{

		address = InetAddress.getByName("192.168.0.107");//InetAddress.getLocalHost();
		nodeIP = address.getHostAddress();
		nodeID = evaluateID(nodeIP);
		m = hash.length * 8;
		ringDimension = BigInteger.valueOf((long) 2).pow(m);
		
		for(int i = 0; i < m; i++) {
			BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(i)).mod(ringDimension);
			InetAddress entryValue = null;
			fingerTable.put(entryKey, entryValue);
		}		
		//System.out.println("nodeIP:\t" + nodeIP + "\nm:\t" + m + "\nnodeID:\t" + nodeID + "\n" + digest.toString());
		/*
		for (BigInteger key: fingerTable.keySet()) {
            System.out.println(key + "\t" + fingerTable.get(key));
		}
		 */
		
		printNodeInformation();
		
		port = 6007;
		serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress(address, 6007));
		listener = new Listener(serverSocket, this);
		new Thread(listener).start();
		
	}

	public void findSuccessor(InetAddress node, String originalSender) throws NoSuchAlgorithmException, IOException {
		/*
		 * 
		 * 
		 * Args:
		 * 		node:
		 * 		originalSender:
		 */
		System.out.println(nodeID + " " + evaluateID(successor.getHostAddress()) + " " + evaluateID(closestPrecedingNode(node).getHostAddress()));
		if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 0)
			createJSON(Command.SUCC_RES, originalSender, originalSender, successor.getHostAddress());
		else if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 1) {
			if((evaluateID(node.getHostAddress()).compareTo(nodeID) == 1
						&& evaluateID(node.getHostAddress()).compareTo(ringDimension) == -1)
					|| (evaluateID(node.getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
						&& evaluateID(node.getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) != 1))
				createJSON(Command.SUCC_RES, originalSender, originalSender, successor.getHostAddress());
		} else if(evaluateID(node.getHostAddress()).compareTo(nodeID) == 1
				&& evaluateID(node.getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) != 1)
			createJSON(Command.SUCC_RES, originalSender, originalSender, successor.getHostAddress());
		else
			createJSON(Command.SUCC_REQ, originalSender, closestPrecedingNode(node).getHostAddress(), null);
	}

	private InetAddress closestPrecedingNode(InetAddress node) throws UnknownHostException, NoSuchAlgorithmException { //gestire i casi con fingerTable.get(i).getNodeID() == NULL
		for(int i = m; i > 0; i--) {
			if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 1)
				if((evaluateID(fingerTable.get(i).getHostAddress()).compareTo(nodeID) == 1
							&& evaluateID( fingerTable.get(i).getHostAddress()).compareTo(ringDimension) == -1)
						|| (evaluateID(fingerTable.get(i).getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
							&& evaluateID(fingerTable.get(i).getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) != 1))
					return fingerTable.get(i);
			if(fingerTable.get(i) != null 
					&& evaluateID(fingerTable.get(i).getHostAddress()).compareTo(nodeID) == 1
					&& evaluateID(fingerTable.get(i).getHostAddress()).compareTo(nodeID) == -1)
				return fingerTable.get(i);
		}
		return address;
	}	 
	//address � l'indirizzo trovato da inoltrare a chi ha fatto richiesta
	public void createJSON(Command command, String originalSender, String receiver, String address) throws IOException {
		/*
		 * 
		 * 
		 * Args:
		 * 		command: tag that define the command to execute
		 * 		originalSender: IP of the node that originally send the request
		 * 		receiver: IP of the node that receive the request
		 * 		address: the node identifier found to forward to the request sender
		 */
		Socket socket = new Socket(receiver, port);
		PrintStream out = new PrintStream(socket.getOutputStream());
		JSONObject json = new JSONObject();
		json.put("op_code", command);
		json.put("original_sender", originalSender);
		json.put("current_sender", this.nodeIP);
		json.put("address", address);
		try {
			out.println(json.toString());
		}finally {
			socket.close();
		}
	}
	
	public void create() throws UnknownHostException {
		/*
		 * Initializes a new ring
		 */
		predecessor = address; //con NULL crasha al primo stabilize
		successor = address;
		Timer timer = new Timer();
		Stabilizer stabilizer = new Stabilizer(this);
		timer.schedule(stabilizer, 0, 5000);
		System.out.println("New ring created.");
	}	

	public void join(InetAddress address) throws UnknownHostException, IOException, InterruptedException {
		predecessor = null;
		System.out.println("Trying to join the ring");
		createJSON(Command.JOIN, this.nodeIP, address.getHostAddress(), null);
		synchronized(this) {
			wait();
		}
		
		Timer timer = new Timer();
		Stabilizer stabilizer = new Stabilizer(this);
		timer.schedule(stabilizer, 0, 5000);
	}

	public void leave() throws IOException {
		serverSocket.close();
	}
	
	public void stabilize() throws InterruptedException, IOException {
		//chiedo al mio sucessore chi � il suo predecessore e vado in wait
		createJSON(Command.PRED_REQ, this.nodeIP, successor.getHostAddress(), null);
		synchronized(this) {
			wait();
		}
		
		System.out.println("successor: " + successor + "\npredecessor: " + predecessor + "\nsuccessor predecessor: " + succPred.toString());
		/*
		if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 1)
			if((evaluateID(node.getHostAddress()).compareTo(nodeID) == 1
						&& evaluateID(node.getHostAddress()).compareTo(ringDimension) == -1)
					|| (evaluateID(node.getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
						&& evaluateID(node.getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) == -1))
				successor = node;
		
		if(evaluateID(node.getHostAddress()).compareTo(nodeID) == 1
				&& evaluateID(node.getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) == -1)
			successor = node;
		
		//this.successor.notify(this.address);
		createJSON(Command.NOTIFY, address);*/
	}

	public void notify(InetAddress node) throws NoSuchAlgorithmException {
		if(predecessor != null && evaluateID(predecessor.getHostAddress()).compareTo(nodeID) == 1)
			if((evaluateID(node.getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) == 1
						&& evaluateID(node.getHostAddress()).compareTo(ringDimension) == -1)
					|| (evaluateID(node.getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
						&& evaluateID(node.getHostAddress()).compareTo(nodeID) == -1))
				predecessor = node;
		if(predecessor == null
					|| (evaluateID(node.getHostAddress()).compareTo(evaluateID(predecessor.getHostAddress())) == 1
						&& evaluateID(node.getHostAddress()).compareTo(nodeID) == -1))
			predecessor = node;
	}

	public void fixFingers() {

	}

	public void checkPredecessor() throws UnknownHostException, IOException {
		if(InetAddress.getByName(predecessor.getHostAddress()).isReachable(2000))
			predecessor = null;
	}
	 
	private BigInteger evaluateID(String nodeIP) throws NoSuchAlgorithmException {
		digest = MessageDigest.getInstance("SHA-1");
		hash = digest.digest(nodeIP.getBytes(StandardCharsets.UTF_8));
		//System.out.println(hash);
		
		return new BigInteger(1, hash);
	}
	
	private void printNodeInformation() throws NoSuchAlgorithmException {
		System.out.println("NodeIP:\t" + nodeIP + "\nm:\t" + m + "\nNodeID:\t" + nodeID + "\n" + digest.toString());
		if(successor != null)
			System.out.println("Successor ID\t\t" + evaluateID(successor.getHostAddress()));
		if(predecessor != null)
			System.out.println("Predecessor ID\t\t" + evaluateID(predecessor.getHostAddress()));

		//for (BigInteger key: fingerTable.keySet()) {
        //    System.out.println(key + "\t" + fingerTable.get(key));
		//}
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
	
	public void setSuccPred(InetAddress succPred) {
		this.succPred = succPred;
	}
	 
	public static void main (String[] args) throws NoSuchAlgorithmException, IOException, InterruptedException {
		Node n1 = new Node();
		int choice;
		System.out.println("1. create\n2. join\n");
		Scanner stdin = new Scanner(System.in);
		choice = Integer.valueOf(stdin.nextLine());
		switch(choice) {
		case 1:
			n1.create();
			break;
		case 2:
			System.out.println("IP to join: ");
			n1.join(InetAddress.getByName(stdin.nextLine()));
			stdin.close();
			break;
		}
		//Node n2 = new Node();
		//Node n2 = new Node();
		//n1.create();
		//n2.join(n1);
		//n2.setNodeID(n2.getNodeID().add(BigInteger.valueOf((long) 1)));
		//System.out.println("New N2 ID: " + n2.getNodeID());
		//n1.stabilize();
		//n2.stabilize();
		//System.out.println("N1 ID: " + n1.getNodeID() + "\nN1 successor: " + evaluateID(n1.successor.getHostAddress()) + "\nN1 predecessor: " + evaluateID(n1.predecessor.getHostAddress());
		//System.out.println("N2 ID: " + n2.getNodeID() + "\nN2 successor: " + n2.successor.getNodeID() + "\nN2 predecessor: " + n2.predecessor.getNodeID());	
		//n1.stabilize();
		//n2.join(n1);
		//System.out.println(n1.successor);
	}
}