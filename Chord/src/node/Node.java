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

/**
 * 
 * @author Matteo Vantadori, Ivan Sanzeni
 *
 */
public class Node {
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
	private InetAddress succSucc;
	
	private InetAddress lookupResponse;
	private Object lookupSync;

	private int next;
	private LinkedHashMap<BigInteger, InetAddress> fingerTable;
	
	private ServerSocket serverSocket;
	private Thread listener;
	private Stabilizer stabilizer;
	private PredecessorChecker checker;
	private Fixer fixer;
	
	public Node() throws IOException, NoSuchAlgorithmException{

		address = InetAddress.getLocalHost();
		nodeIP = address.getHostAddress();
		nodeID = evaluateID(nodeIP);
		m = hash.length * 8;
		ringDimension = BigInteger.valueOf((long) 2).pow(m);

		succPred = null;
		succSucc = null;
		
		lookupSync = new Object();
		
		next = 0;
		fingerTable = new LinkedHashMap<BigInteger, InetAddress>();
		
		for(int i = 0; i < m; i++) {
			BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(i)).mod(ringDimension);
			//InetAddress entryValue = null;
			fingerTable.put(entryKey, address);
		}
	}

	/**
	 * 
	 * @param node
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public synchronized InetAddress findSuccessor(BigInteger node) throws NoSuchAlgorithmException {
		if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 0)
			return successor;
		//myID > succID && (nodeID > myID || nodeID <= succID)
		else if(nodeID.compareTo(evaluateID(successor.getHostAddress())) == 1
				&& (node.compareTo(nodeID) == 1 || node.compareTo(evaluateID(successor.getHostAddress())) != 1))
				return successor;
		//nodeID > myID && nodeID <= succID
		else if(node.compareTo(nodeID) == 1 && node.compareTo(evaluateID(successor.getHostAddress())) != 1)
			return successor;
		else {
			return null;
		}
	}

	/**
	 * 
	 * @param node
	 * @return
	 * @throws UnknownHostException
	 * @throws NoSuchAlgorithmException
	 */
	public InetAddress closestPrecedingNode(BigInteger node) throws NoSuchAlgorithmException {
		BigInteger entryKey;
		for(int i = m; i > 0; i--) {
			entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(i-1)).mod(ringDimension);
			
			//myID > nodeID && (fingerID[i] > myID || fingerID[i] < nodeID)
			if(nodeID.compareTo(node) == 1
				&& (evaluateID(fingerTable.get(entryKey).getHostAddress()).compareTo(nodeID) == 1
						|| evaluateID(fingerTable.get(entryKey).getHostAddress()).compareTo(node) == -1)) 
					return fingerTable.get(entryKey);
			
			//fingerID[i] > myID && fingerID[i] < nodeID
			else if(evaluateID(fingerTable.get(entryKey).getHostAddress()).compareTo(nodeID) == 1
					&& evaluateID(fingerTable.get(entryKey).getHostAddress()).compareTo(node) == -1)
				return fingerTable.get(entryKey);
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
	public void createJSON(Command command, String originalSender, String receiver, String payload) throws IOException {
		Socket socket = new Socket();
		socket.connect(new InetSocketAddress(receiver, 6007), 1000);
		PrintStream out = new PrintStream(socket.getOutputStream());
		JSONObject json = new JSONObject();
		json.put("op_code", command);
		json.put("original_sender", originalSender);
		json.put("payload", payload);
		try {
			out.println(json.toString());
		}finally {
			socket.close();
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	public boolean create() {
		try {
			predecessor = null;
			successor = address;
			
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(address, 6007));
			listener = new Thread(new Listener(serverSocket, this, lookupSync));
			listener.start();
		
			Timer timer = new Timer();
			stabilizer = new Stabilizer(this);
			timer.schedule(stabilizer, 0, 10000);
			checker = new PredecessorChecker(this);
			timer.schedule(checker, 0, 5000);
			fixer = new Fixer(this);
			timer.schedule(fixer, 0, 100);
			//System.out.println("Created");
			return true;
		} catch(IOException e) {
			return false;
		}
	}	

	/**
	 * 
	 * @param address
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public boolean join(InetAddress address) throws InterruptedException {
		try {
			predecessor = null;
			System.out.println("Trying to join the ring");
			
			serverSocket = new ServerSocket();
			serverSocket.bind(new InetSocketAddress(this.address, 6007));
			listener = new Thread(new Listener(serverSocket, this, lookupSync));
			listener.start();
			
			createJSON(Command.JOIN, this.nodeIP, address.getHostAddress(), null);
	
			synchronized(this) {
				wait();
			}
	
			Timer timer = new Timer();
			stabilizer = new Stabilizer(this);
			fixer = new Fixer(this);
			checker = new PredecessorChecker(this);
			timer.schedule(stabilizer, 0, 1000);
			timer.schedule(checker, 0, 500);
			timer.schedule(fixer, 0, 100);
			return true;
		} catch(IOException e) {
			return false;
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	public void leave() throws IOException {
		checker.cancel();
		fixer.cancel();
		serverSocket.close();
		listener.interrupt();
		stabilizer.cancel();
	}

	/**
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public void stabilize() throws InterruptedException, NoSuchAlgorithmException, IOException {
		try {
			createJSON(Command.PRED_REQ, nodeIP, successor.getHostAddress(), null);
		
		
		synchronized(this) {
			wait(5000);
		}
		

			createJSON(Command.SS_REQ, nodeIP, successor.getHostAddress(), null);

		synchronized(this) {
			wait(5000);		}

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
		//printNodeInformation();
			createJSON(Command.NOTIFY, nodeIP, successor.getHostAddress(), null);
		} catch(IOException e) {
			//System.out.println(successor.getHostAddress());
			if(successor == succSucc)
				leave();
			else
				successor = succSucc;
			return;
		}
	}
	
	/**
	 * 
	 * @param node
	 * @throws NoSuchAlgorithmException
	 */
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

	/**
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public void fixFingers() throws NoSuchAlgorithmException {
		next++;
		if(next > m)
			next = 1;
		BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(next-1)).mod(ringDimension);
		InetAddress t = findSuccessor(entryKey);
		if(t == null)
			try {
			createJSON(Command.SUCC_REQ, nodeIP, closestPrecedingNode(entryKey).getHostAddress(), Integer.toString(next));
			} catch(IOException e) {
				return;
			}
			
		else
			fingerTable.replace(entryKey, t);
		//System.out.println("FIX FINGERS");
		//printNodeInformation();
	}

	/**
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	public synchronized void checkPredecessor() throws NoSuchAlgorithmException {
		if(predecessor != null)
			try {
				createJSON(Command.CHECK, nodeIP, predecessor.getHostAddress(), null);
			} catch(IOException | NullPointerException e) {
				predecessor = null;
			}
		//System.out.println("CHECK PREDECESSOR");
	}

	/**
	 * 
	 * @param nodeIP
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public BigInteger evaluateID(String nodeIP) throws NoSuchAlgorithmException {
		digest = MessageDigest.getInstance("SHA-1");
		hash = digest.digest(nodeIP.getBytes(StandardCharsets.UTF_8));
		return new BigInteger(1, hash);
	}

	/**
	 * 
	 * @param payload
	 * @throws UnknownHostException
	 */
	public void setFinger(String payload) throws UnknownHostException {
		String[] elem = payload.split("@");
		InetAddress entryValue = InetAddress.getByName(elem[0]);
		int next = Integer.valueOf(elem[1]);
		BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(next - 1)).mod(ringDimension);
		fingerTable.replace(entryKey, entryValue);
	}
	
	/**
	 * 
	 * @throws NoSuchAlgorithmException
	 */
	public void printNodeInformation() throws NoSuchAlgorithmException {
		System.out.println("NodeIP:\t" + nodeIP + "\nm:\t" + m + "\nNodeID:\t" + nodeID + "\n" + digest.toString());
		if(successor != null)
			System.out.println("SuccessorID:\t\t" + evaluateID(successor.getHostAddress()));
		else
			System.out.println("SuccessorID:\t\tnull");
		if(predecessor != null)
			System.out.println("PredecessorID:\t\t" + evaluateID(predecessor.getHostAddress()));
		else
			System.out.println("PredecessorID:\t\tnull");
		if(succSucc != null)
			System.out.println("SuccSuccessorID:\t" + evaluateID(succSucc.getHostAddress()));
		else
			System.out.println("SuccSuccessorID:\tnull");
		if(succPred != null)
			System.out.println("SuccPredecessorID:\t" + evaluateID(succPred.getHostAddress()));
		else
			System.out.println("SuccPredecessorID:\tnull");
		/*System.out.println("________________________________________________________________________");
		for (BigInteger key: fingerTable.keySet())
				System.out.format("%49s%32s\n", key, fingerTable.get(key));
		System.out.println("__________________________________________________________________________");
		*/
	}
	
	public void printFingerTable() throws NoSuchAlgorithmException {
		System.out.println("_________________________________________________________________________________\n");
		for (BigInteger key: fingerTable.keySet())
			System.out.format("%49s%32s\n", key, fingerTable.get(key));
	}
	
	/**
	 * 
	 * @param key
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public InetAddress lookup(String key) throws NoSuchAlgorithmException, InterruptedException, IOException {
		BigInteger id = evaluateID(key);
		if(findSuccessor(id) != null)
			return findSuccessor(id);
		else
			createJSON(Command.LOOKUP_REQ, nodeIP, closestPrecedingNode(id).getHostAddress(), id.toString());

		synchronized(lookupSync) {
			lookupSync.wait();
		}
		
		return lookupResponse;
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

	public BigInteger getRingDimension() {
		return ringDimension;
	}
	
	public void setSuccessor(InetAddress successor) {
		this.successor = successor;
	}

	public void setSuccPred(InetAddress succPred) {
		this.succPred = succPred;
	}
	
	public void setSuccSucc(InetAddress succSucc) {
		this.succSucc = succSucc;
	}
	
	public void setLookupResponse(InetAddress response) {
		this.lookupResponse = response;
	}

	public static void main (String[] args) throws NoSuchAlgorithmException, IOException, InterruptedException {
		Node n1;
		boolean t = false;
		int choice;
		Scanner stdin = new Scanner(System.in);
		while(!t) {
			System.out.println("1. create\n2. join\n");
			choice = Integer.valueOf(stdin.nextLine());
			switch(choice) {
			case 1:
				n1 = new Node();
				System.out.println("caso 1");
				t = n1.create();
				break;
			case 2:
				n1 = new Node();
				System.out.println("IP to join: ");
				t = n1.join(InetAddress.getByName(stdin.nextLine()));
				break;
			}
			choice = Integer.valueOf(stdin.nextLine());
			System.out.println(choice);
			stdin.reset();
		}
		stdin.close();
	}
}