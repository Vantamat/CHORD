package node;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.TimerTask;

public class Stabilizer extends TimerTask{
	private Node node;
	
	public Stabilizer(Node node) {
		this.node = node;
	}
	
	public void run() {
		try {
			node.stabilize();
		} catch (InterruptedException | NoSuchAlgorithmException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("Predecessor: " + node.getPredecessor() + "\nSuccessor: " + node.getSuccessor());
	}
}
