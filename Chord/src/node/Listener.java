package node;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Listener continua a stare in ascolto per richieste di connessione provenienti da altri nodi della rete,
 * quando riceve una richiesta crea un nuovo processo per gestire le richieste del nodo (join, stabilize, ...)
 */
public class Listener implements Runnable{
	private ServerSocket serverSocket;
	private Node node;
	private Object lookupSync;
	
	
	/**
	 * @param serverSocket: la serverSocket che sarà utilizzata per essere contattati da altri nodi
	 */
	public Listener(ServerSocket serverSocket, Node node, Object lookupSync) {
		this.serverSocket = serverSocket;
		this.node = node;
		this.lookupSync = lookupSync;
	}

	@Override
	public void run() {
		try {
			while(true) {
				Socket socket = serverSocket.accept();
				new Thread(new RequestHandler(socket, node, lookupSync)).start();
			}
		} catch (SocketException e) {
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
