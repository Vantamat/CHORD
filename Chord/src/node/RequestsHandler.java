package node;

import java.net.Socket;

/**
 * RequestHandler viene generata una volta ricevuta una richiesta di connessione da un nodo della rete. La classe si occupa
 * della gestione della richiesta di tale nodo.
 */
public class RequestsHandler implements Runnable{
	private Socket socket;
	/**
	 * @param socket: la socket che dovrà essere utilizzata per comunicare con il nodo che ha richiesto la connessione
	 */
	public RequestsHandler(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
