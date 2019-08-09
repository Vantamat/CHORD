package node;

import java.util.TimerTask;

public class Scheduled extends TimerTask{
	private Node node;	
	
	public Scheduled(Node node) {
		this.node = node;
	}
	
	public void run() {
		node.stabilize();
	}
}
