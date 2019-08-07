package node;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * RequestHandler viene generata una volta ricevuta una richiesta di connessione da un nodo della rete. La classe si occupa
 * della gestione della richiesta di tale nodo.
 */
public class RequestsHandler implements Runnable{
	private Socket socket;
	/**
	 * @param socket: la socket che dovr� essere utilizzata per comunicare con il nodo che ha richiesto la connessione
	 */
	public RequestsHandler(Socket socket) {
		this.socket = socket;
	}
	
	/**
	 * in questo run il nodo rester� in ascolto dei messaggi provenienti dal nodo che ha aperto la comunicazione, da quest'ultimo 
	 * ricever� delle stringhe (enum?) ed in base al loro valore verranno eseguite diverse operazioni
	 */
	@Override
	public void run() {
		Command line;
		Scanner in;
		try {
			in = new Scanner(socket.getInputStream());
			line = Command.valueOf(in.nextLine());
			
			switch(line) {
			case JOIN:
				break;
			case SUCC:
				break;
			case PRED:
				break;
			case NOTIFY:
				break;
			}
			
			in.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
