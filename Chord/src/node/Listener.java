package node;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Listener continua a stare in ascolto per richieste di connessione provenienti da altri nodi della rete,
 * quando riceve una richiesta crea un nuovo processo per gestire le richieste del nodo (join, stabilize, ...)
 */

public class Listener implements Runnable{
	private ServerSocket serverSocket;
	private Node node;
	
	
	/**
	 * @param serverSocket: la serverSocket che sar� utilizzata per essere contattati da altri nodi
	 */
	public Listener(ServerSocket serverSocket, Node node) {
		this.serverSocket = serverSocket;
		this.node = node;
	}
	
	@Override
	public void run() {
		try {
			while(true) {
				System.out.println("Waiting for connections");
				new Thread(new RequestsHandler(serverSocket.accept(), node)).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			
		}
	}

}
