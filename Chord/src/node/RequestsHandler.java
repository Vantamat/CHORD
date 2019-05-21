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
	 * @param socket: la socket che dovrà essere utilizzata per comunicare con il nodo che ha richiesto la connessione
	 */
	public RequestsHandler(Socket socket) {
		this.socket = socket;
	}
	
	/**
	 * in questo run il nodo resterà in ascolto dei messaggi provenienti dal nodo che ha aperto la comunicazione, da quest'ultimo 
	 * riceverà delle stringhe (enum?) ed in base al loro valore verranno eseguite diverse operazioni
	 */
	@Override
	public void run() {
		String line;
		Scanner in;
		try {
			in = new Scanner(socket.getInputStream());
			line = in.nextLine();
			
			switch(line) {
			case("1"):
				break;
			case("2"):
				break;
			}
			
			in.close();
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
