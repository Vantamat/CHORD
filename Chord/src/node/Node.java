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
	 *	 	port:
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

	private InetAddress succPred = null;

	private LinkedHashMap<BigInteger, InetAddress> fingerTable = new LinkedHashMap<BigInteger, InetAddress>();

	private int port;
	private ServerSocket serverSocket;
	private Listener listener;

	public Node() throws IOException, NoSuchAlgorithmException{

		address = InetAddress.getByName("192.168.43.151");//InetAddress.getLocalHost();
		nodeIP = address.getHostAddress();
		nodeID = evaluateID(nodeIP);
		m = hash.length * 8;
		ringDimension = BigInteger.valueOf((long) 2).pow(m);

		for(int i = 0; i < m; i++) {
			BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(i)).mod(ringDimension);
			InetAddress entryValue = null;
			fingerTable.put(entryKey, entryValue);
		}		
		System.out.println("nodeIP\t" + nodeIP + "\nm\t" + m + "\nnodeID\t" + nodeID + "\n" + digest.toString());
		/*
		for (BigInteger key: fingerTable.keySet()) {
            System.out.println(key + "\t" + fingerTable.get(key));
		}
		 */

		//printNodeInformation();

		port = 6007;
		serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress(address, 6007));
		listener = new Listener(serverSocket, this);
		new Thread(listener).start();

	}

	public void findSuccessor(InetAddress node, String originalSender) throws NoSuchAlgorithmException, IOException {
		//System.out.println(nodeID + " " + evaluateID(successor.getHostAddress()) + " " + evaluateID(closestPrecedingNode(node).getHostAddress()));
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

	/**
	 * Create and send a JSON object to a specified node via socket
	 * @param command - the command that the receiving node need to perform
	 * @param originalSender - IP address of the request sender
	 * @param receiver - IP address of the node to which we want to send the JSON
	 * @param address - a field containing the result of an operation if needed, null otherwise
	 * @throws IOException
	 */
	public void createJSON(Command command, String originalSender, String receiver, String address) throws IOException {
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
		predecessor = null;//address; //con NULL crasha al primo stabilize
		successor = address;
		Timer timer = new Timer();
		Stabilizer stabilizer = new Stabilizer(this);
		timer.schedule(stabilizer, 0, 10000);
		System.out.println("Created");
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
		timer.schedule(stabilizer, 0, 10000);
	}

	public void leave() throws IOException {
		serverSocket.close();
	}

	public void stabilize() throws InterruptedException, IOException, NoSuchAlgorithmException {
		//chiedo al mio sucessore chi � il suo predecessore e vado in wait
		createJSON(Command.PRED_REQ, this.nodeIP, successor.getHostAddress(), null);

		synchronized(this) {
			wait();
			//ricevuto in notify succPred � stato aggiornato
		}
		
		//myID > succID && (succPredID > myID || succPredID < succID)
		if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 1 &&
				(evaluateID(succPred.getHostAddress()).compareTo(nodeID) == 1 || 
				evaluateID(succPred.getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) == -1)) 
		{
			successor = succPred;
		}
		
		//succID > myID && myID < succPredID < succID
		else if(evaluateID(successor.getHostAddress()).compareTo(nodeID) == 1 && 
				evaluateID(succPred.getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) == -1 &&
				evaluateID(succPred.getHostAddress()).compareTo(nodeID) == -1) 
		{
			successor = succPred;
		}

		System.out.println("\nsuccessor: " + successor + "\npredecessor: " + predecessor + "\nsuccessor predecessor: " + succPred + "\n");
		
		createJSON(Command.NOTIFY, nodeIP, successor.getHostAddress(), null);
	}
	
	
	public void notify(InetAddress node) throws NoSuchAlgorithmException {
		if(predecessor == null)
			predecessor = node;
		
		//predID > myID && (nodeID < myID || nodeID > predID)
		else if(evaluateID(predecessor.getHostAddress()).compareTo(nodeID) == 1 &&
				(evaluateID(node.getHostAddress()).compareTo(nodeID) == -1 ||
				evaluateID(node.getHostAddress()).compareTo(evaluateID(predecessor.getHostAddress())) == 1)) 
		{
			predecessor = node;
		}
		
		//predID < myID && nodeID < myID && nodeID > predID
		else if(evaluateID(predecessor.getHostAddress()).compareTo(nodeID) ==  -1 &&
				evaluateID(node.getHostAddress()).compareTo(nodeID) == -1 &&
				evaluateID(node.getHostAddress()).compareTo(evaluateID(predecessor.getHostAddress())) == 1) 
		{
			predecessor = node;
		}
		
		/*if(predecessor != null && evaluateID(predecessor.getHostAddress()).compareTo(nodeID) == 1)
			if((evaluateID(node.getHostAddress()).compareTo(evaluateID(successor.getHostAddress())) == 1
			&& evaluateID(node.getHostAddress()).compareTo(ringDimension) == -1)
					|| (evaluateID(node.getHostAddress()).compareTo(BigInteger.valueOf((long) 0)) != -1
					&& evaluateID(node.getHostAddress()).compareTo(nodeID) == -1))
				predecessor = node;
		if(predecessor == null
				|| (evaluateID(node.getHostAddress()).compareTo(evaluateID(predecessor.getHostAddress())) == 1
				&& evaluateID(node.getHostAddress()).compareTo(nodeID) == -1))
			predecessor = node;*/
	}

	public void fixFingers() {

	}

	public void checkPredecessor() throws UnknownHostException, IOException {
		if(InetAddress.getByName(predecessor.getHostAddress()).isReachable(2000))
			predecessor = null;
	}

	/**
	 * Given the IP address of a node return its ID in the ring
	 */
	private BigInteger evaluateID(String nodeIP) throws NoSuchAlgorithmException {
		digest = MessageDigest.getInstance("SHA-1");
		hash = digest.digest(nodeIP.getBytes(StandardCharsets.UTF_8));

		return new BigInteger(1, hash);
	}

	private void printNodeInformation() throws NoSuchAlgorithmException {
		System.out.println("Node IP\t\t" + nodeIP + "\nID dimension\t" + m + "\nNode ID\t\t" + nodeID + "\n" + digest.toString());
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
			break;
		}
		//Node n2 = new Node();
		//Node n2 = new Node();1
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