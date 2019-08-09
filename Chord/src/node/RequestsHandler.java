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
			//JSONParser parser = new JSONParser();
			Scanner in = new Scanner(socket.getInputStream());
			System.out.println("SI PIANTA A NEXT LINE");
			String j = in.nextLine();
			System.out.println("SI PIANTA A JSONOBJECT");
			JSONObject json = new JSONObject(j);
			System.out.println(j);
			//JSONObject json = (JSONObject) parser.parse(j);
			
			switch(Command.valueOf((String) json.get("op_code"))) {
			case JOIN:
				System.out.println("Join");
				node.findSuccessor(InetAddress.getByName(json.get("address").toString()));
				break;
			case SUCC:
				System.out.println("Succ");
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
