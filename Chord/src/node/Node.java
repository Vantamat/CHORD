package node;

import java.math.BigInteger;
import java.net.InetAddress;
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
	
	LinkedHashMap<BigInteger, BigInteger> fingerTable = new LinkedHashMap<BigInteger, BigInteger>();

	public Node() throws UnknownHostException, NoSuchAlgorithmException{
		
		nodeIP = InetAddress.getLocalHost().getHostAddress();
		
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] hash = digest.digest(nodeIP.getBytes(StandardCharsets.UTF_8));

		BigInteger nodeID = new BigInteger(1, hash);
		
		m = hash.length * 8;
		ringDimension = BigInteger.valueOf((long) 2).pow(m);
		
		for(int i = 0; i < m; i++) {
			BigInteger entryKey = nodeID.add(BigInteger.valueOf((long) 2).pow(i)).mod(ringDimension);
			BigInteger entryValue = null;
			fingerTable.put(entryKey, entryValue);
		}		
		System.out.println("nodeIP\t" + nodeIP + "\nm\t" + m + "\nnodeID\t" + nodeID + "\n" + digest.toString());
	
		for (BigInteger name: fingerTable.keySet()) {
            System.out.println(name);
		} 
	}
	
	public void create() {
		predecessor = null;
		successor = this.nodeID;
	}	
	
	public static void main (String[] args) throws UnknownHostException, NoSuchAlgorithmException {
		new Node();
	}
}