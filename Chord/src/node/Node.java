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

	private int next;
	private LinkedHashMap<BigInteger, InetAddress> fingerTable;

	private int port;
	private ServerSocket serverSocket;
	private Listener listener;

	public Node() throws IOException, NoSuchAlgorithmException{

		address = InetAddress.getByName("192.168.43.151");//InetAddress.getLocalHost();
		nodeIP = address.getHostAddress();
		nodeID = evaluateID(nodeIP);
		m = hash.length * 8;
		ringDimension = BigInteger.valueOf((long) 2).pow(m);

		succPred = null;
		
		next = 0;
		fingerTable = new LinkedHashMap<BigInteger, InetAddress>();
		
		for(int i = 0; i < m; i++) {
			BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(i)).mod(ringDimension);
			//InetAddress entryValue = null;
			fingerTable.put(entryKey, address);
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

	public InetAddress findSuccessor(BigInteger node, String originalSender, String fixTag) throws NoSuchAlgorithmException, IOException {		
		if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 0)
			return successor;
		//myID > succID && (nodeID > myID || nodeID <= succID)
		else if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 1
				&& (node.compareTo(nodeID) == 1 || node.compareTo(evaluateID(successor.getHostAddress())) != 1))
				return successor;
				//createJSON(Command.SUCC_RES, originalSender, originalSender, successor.getHostAddress());
		//nodeID > myID && nodeID <= succID
		else if(node.compareTo(nodeID) == 1 && node.compareTo(evaluateID(successor.getHostAddress())) != 1)
			return successor;
			//createJSON(Command.SUCC_RES, originalSender, originalSender, successor.getHostAddress());
		else {
			/*if(closestPrecedingNode(node).equals(address) || fingerTable.get(nodeID.add(BigInteger.valueOf((long) 2).pow(next-1)).mod(ringDimension)).equals(address)) {
				System.out.println("CASO A " + closestPrecedingNode(node).getHostAddress());
				return address;
			}
			else {*/
				return null;
				/*System.out.println("CASO B");
				if(fingerTable.get(node) == null)//fixTag == null)
					return closestPrecedingNode(node);
				else
					return closestPrecedingNode(node);//fingerTable.get(node);//nodeID.add(BigInteger.valueOf((long) 2).pow(next-1)).mod(ringDimension));*/
			//}
		}
	}

	public InetAddress closestPrecedingNode(BigInteger node) throws UnknownHostException, NoSuchAlgorithmException {
		BigInteger entryKey;
		for(int i = m; i > 0; i--) {
			entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(i-1)).mod(ringDimension);
			if(fingerTable.get(entryKey) != null) {
				//myID > nodeID && (fingerID[i] > myID || fingerID[i] < nodeID)
				if(nodeID.compareTo(node) == 1
					&& (evaluateID(fingerTable.get(entryKey).getHostAddress()).compareTo(nodeID) == 1
							|| evaluateID(fingerTable.get(entryKey).getHostAddress()).compareTo(node) == -1)) {
						return fingerTable.get(entryKey);
				}
				//fingerID[i] > myID && fingerID[i] < nodeID
				else if(evaluateID(fingerTable.get(entryKey).getHostAddress()).compareTo(nodeID) == 1
						&& evaluateID(fingerTable.get(entryKey).getHostAddress()).compareTo(node) == -1) {
					return fingerTable.get(entryKey);
				}
			}
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
		predecessor = null;
		successor = address;
		Timer timer = new Timer();
		Stabilizer stabilizer = new Stabilizer(this);
		timer.schedule(stabilizer, 0, 10000);
		PredecessorChecker checker = new PredecessorChecker(this);
		timer.schedule(checker, 0, 5000);
		Fixer fixer = new Fixer(this);
		timer.schedule(fixer, 0, 100);
		System.out.println("Created");
	}	

	public void join(InetAddress address) throws IOException, InterruptedException {
		predecessor = null;
		System.out.println("Trying to join the ring");
		createJSON(Command.JOIN, this.nodeIP, address.getHostAddress(), null);

		synchronized(this) {
			wait();
		}

		Timer timer = new Timer();
		Stabilizer stabilizer = new Stabilizer(this);
		Fixer fixer = new Fixer(this);
		PredecessorChecker checker = new PredecessorChecker(this);
		timer.schedule(stabilizer, 0, 10000);
		timer.schedule(checker, 0, 5000);
		timer.schedule(fixer, 0, 100);
		
	}

	public void leave() throws IOException {
		serverSocket.close();
	}

	public void stabilize() throws InterruptedException, IOException, NoSuchAlgorithmException {
		//chiedo al mio sucessore chi e' il suo predecessore e vado in wait
		createJSON(Command.PRED_REQ, this.nodeIP, successor.getHostAddress(), null);

		synchronized(this) {
			wait();
			//ricevuto in notify succPred e' stato aggiornato
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
				evaluateID(succPred.getHostAddress()).compareTo(nodeID) == 1) 
		{
			successor = succPred;
		}
		else if(evaluateID(successor.getHostAddress()).compareTo(nodeID) == 0)
			if(succPred != null) {
				successor = succPred;				
			}
		printNodeInformation();
		createJSON(Command.NOTIFY, nodeIP, successor.getHostAddress(), null);
	}


	public synchronized void notify(InetAddress node) throws NoSuchAlgorithmException {
		if(predecessor == null)
			predecessor = node;

		//predID > myID && (nodeID < myID || nodeID > predID)
		else if(evaluateID(predecessor.getHostAddress()).compareTo(nodeID) == 1 &&
				(evaluateID(node.getHostAddress()).compareTo(nodeID) == -1 ||
				evaluateID(node.getHostAddress()).compareTo(evaluateID(predecessor.getHostAddress())) == 1)) 
			predecessor = node;

		//predID < myID && nodeID < myID && nodeID > predID
		else if(evaluateID(predecessor.getHostAddress()).compareTo(nodeID) ==  -1 &&
				evaluateID(node.getHostAddress()).compareTo(nodeID) == -1 &&
				evaluateID(node.getHostAddress()).compareTo(evaluateID(predecessor.getHostAddress())) == 1) 
			predecessor = node;

		else if(evaluateID(predecessor.getHostAddress()).compareTo(nodeID) ==  0)
			predecessor = node;
		//printNodeInformation();
	}

	public void fixFingers() throws NoSuchAlgorithmException, IOException {
		next++;
		if(next > m)
			next = 1;
		BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(next-1)).mod(ringDimension);
		InetAddress t = findSuccessor(entryKey, nodeIP, "fix");
		if(t == null)
			createJSON(Command.SUCC_REQ, nodeIP, closestPrecedingNode(entryKey).getHostAddress(), "fix");
		else
			fingerTable.replace(entryKey, t);
		//System.out.println("myID\n" + nodeID + "\nactual\n" + entryKey + "\nreturned\n" + evaluateID(t.getHostAddress()));
		
		//findSuccessor(entryKey, nodeIP, "fix");
		//System.out.println("FIX FINGERS");
		//printNodeInformation();
	}

	public synchronized void checkPredecessor() throws NoSuchAlgorithmException {
		if(predecessor != null)
			try {
				createJSON(Command.CHECK, nodeIP, predecessor.getHostAddress(), null);
			} catch(IOException e) {
				predecessor = null;
			}
		//printNodeInformation();
	}

	/**
	 * Given the IP address of a node return its ID in the ring
	 */
	public BigInteger evaluateID(String nodeIP) throws NoSuchAlgorithmException {
		digest = MessageDigest.getInstance("SHA-1");
		hash = digest.digest(nodeIP.getBytes(StandardCharsets.UTF_8));
		return new BigInteger(1, hash);
	}

	public synchronized void setFinger(InetAddress entryValue) {
		BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(next)).mod(ringDimension);
		fingerTable.replace(entryKey, entryValue);
	}
	
	synchronized private void printNodeInformation() throws NoSuchAlgorithmException {
		System.out.println("NodeIP:\t" + nodeIP + "\nm:\t" + m + "\nNodeID:\t" + nodeID + "\n" + digest.toString());
		if(successor != null)
			System.out.println("SuccessorID:\t\t" + evaluateID(successor.getHostAddress()));
		else
			System.out.println("SuccessorID:\t\tnull");
		if(predecessor != null)
			System.out.println("PredecessorID:\t\t" + evaluateID(predecessor.getHostAddress()));
		else
			System.out.println("PredecessorID:\t\tnull");
		if(succPred != null)
			System.out.println("SuccPredecessorID:\t" + evaluateID(succPred.getHostAddress()));
		else
			System.out.println("SuccPredecessorID:\tnull");
		System.out.println("________________________________________________________________________");
		for (BigInteger key: fingerTable.keySet())
				System.out.format("%49s%32s\n", key, fingerTable.get(key));
		System.out.println("________________________________________________________________________");

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