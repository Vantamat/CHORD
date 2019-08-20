package node;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.TimerTask;

public class PredecessorChecker extends TimerTask{
	private Node node;
	
	public PredecessorChecker(Node node) {
		this.node = node;
	}
	
	public void run() {
		try {
			node.checkPredecessor();
		} catch (IOException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("Predecessor: " + node.getPredecessor() + "\nSuccessor: " + node.getSuccessor());
	}
}