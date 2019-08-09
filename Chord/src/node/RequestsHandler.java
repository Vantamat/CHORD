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
			String originalSender = json.get("original_sender").toString();
			String currentSender = json.get("current_sender").toString();
			switch(Command.valueOf((String) json.get("op_code"))) {
			case JOIN:
				System.out.println("Attempt to join");
				System.out.println(currentSender);
				//if(string.charAt(0)=='/')
					//string = string.substring(1, string.length());
				node.findSuccessor(InetAddress.getByName(currentSender), node.getNodeIP());
				break;
			case SUCC_REQ:
				System.out.println("Request to find the successor");
				node.findSuccessor(InetAddress.getByName(currentSender), originalSender);
				break;
			case SUCC_RES:
				System.out.println("Successor found: " + InetAddress.getByName(currentSender));
				if(originalSender == currentSender) {
					node.setSuccessor(InetAddress.getByName(currentSender));
					System.out.println("Successor changed");
				}
				else
					node.createJSON(Command.SUCC_RES, originalSender, node.getNodeIP());
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
