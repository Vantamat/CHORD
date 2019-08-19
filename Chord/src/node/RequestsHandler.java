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
	 * @param socket: la socket che dovr� essere utilizzata per comunicare con il nodo che ha richiesto la connessione
	 */
	public RequestsHandler(Socket socket, Node node) {
		this.socket = socket;
		this.node = node;
	}

	/**
	 * in questo run il nodo rester� in ascolto dei messaggi provenienti dal nodo che ha aperto la comunicazione, da quest'ultimo 
	 * ricever� delle stringhe (enum?) ed in base al loro valore verranno eseguite diverse operazioni
	 */
	@Override
	public void run() {
		try {
			Scanner in = new Scanner(socket.getInputStream());
			String j = in.nextLine();
			JSONObject json = new JSONObject(j);
			String originalSender = json.get("original_sender").toString();
			String currentSender = json.get("current_sender").toString();
			
			System.out.println(json.get("op_code") + " from " + currentSender + " to " + node.getNodeIP());

			switch(Command.valueOf((String) json.get("op_code"))) {
			case JOIN:
				//System.out.println("Attempt to join " + currentSender);
				//if(string.charAt(0)=='/')
					//string = string.substring(1, string.length());
				node.findSuccessor(InetAddress.getByName(currentSender), originalSender);
				break;

			case SUCC_REQ:
				System.out.println("Request to find the successor");
				node.findSuccessor(InetAddress.getByName(currentSender), originalSender);
				break;

			case SUCC_RES:
				System.out.println("Successor found: " + currentSender);
				if(originalSender.compareTo(node.getNodeIP()) == 0) {
					node.setSuccessor(InetAddress.getByName(json.getString("address").toString()));
					System.out.println("Scuccessor changed");
					
					synchronized(node) {
						node.notifyAll();
					}
					
				}
				else
					node.createJSON(Command.SUCC_RES, originalSender, node.getNodeIP(), json.getString("address").toString());
				break;
				
			case PRED_REQ:
				InetAddress temp = node.getPredecessor();
				if(temp == null) 
					node.createJSON(Command.PRED_RES, originalSender, originalSender, null);
				else
					node.createJSON(Command.PRED_RES, originalSender, originalSender, temp.getHostAddress());
				break;

			case PRED_RES:
				if (!json.isNull("address"))
					node.setSuccPred(InetAddress.getByName(json.get("address").toString()));

				synchronized(node) {
					node.notifyAll();
				}
				
				break;

			case NOTIFY:
				node.notify(InetAddress.getByName(originalSender));
				break;
			}

			in.close();
			socket.close();
		} catch (IOException | NoSuchAlgorithmException | JSONException e) {
			e.printStackTrace();
		}
	}

}
