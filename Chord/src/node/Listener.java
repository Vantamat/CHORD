package node;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Listener continua a stare in ascolto per richieste di connessione provenienti da altri nodi della rete,
 * quando riceve una richiesta crea un nuovo processo per gestire le richieste del nodo (join, stabilize, ...)
 */

public class Listener implements Runnable{
	private ServerSocket serverSocket;
	
	
	/**
	 * @param serverSocket: la serverSocket che sarà utilizzata per essere contattati da altri nodi
	 */
	public Listener(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}
	
	@Override
	public void run() {
		try {
			while(true) {
				new Thread(new RequestsHandler(serverSocket.accept())).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
			
		}
	}

}
