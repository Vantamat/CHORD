package node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * RequestHandler viene generata una volta ricevuta una richiesta di connessione da un nodo della rete. La classe si occupa
 * della gestione della richiesta di tale nodo.
 */
public class RequestsHandler implements Runnable{
	private Socket socket;
	private Node node;
	/**
	 * @param socket: la socket che dovrà essere utilizzata per comunicare con il nodo che ha richiesto la connessione
	 */
	public RequestsHandler(Socket socket, Node node) {
		this.socket = socket;
		this.node = node;
	}
	
	/**
	 * in questo run il nodo resterà in ascolto dei messaggi provenienti dal nodo che ha aperto la comunicazione, da quest'ultimo 
	 * riceverà delle stringhe (enum?) ed in base al loro valore verranno eseguite diverse operazioni
	 */
	@Override
	public void run() {
		try {
			System.out.println("Connection recived, handling request...");
			Scanner in = new Scanner(socket.getInputStream());
			String j = in.nextLine();
			JSONObject json = new JSONObject(j);
			System.out.println(j);
			String string = json.get("address").toString();
			System.out.println(string);
			switch(Command.valueOf((String) json.get("op_code"))) {
			case JOIN:
				System.out.println("Attempt to join");
				//if(string.charAt(0)=='/')
					//string = string.substring(1, s.length());
				node.findSuccessor(InetAddress.getByName(string));
				break;
			case SUCC:
				System.out.println("Request to find the successor");
				node.findSuccessor(InetAddress.getByName(string));
				break;
			case PRED:
				break;
			case NOTIFY:
				break;
			}
			
			in.close();
			socket.close();
		} catch (IOException | NoSuchAlgorithmException | JSONException e) {
			e.printStackTrace();
		}
	}

}
