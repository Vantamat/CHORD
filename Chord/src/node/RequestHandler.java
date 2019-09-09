package node;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * RequestHandler viene generata una volta ricevuta una richiesta di connessione da un nodo della rete. La classe si occupa
 * della gestione della richiesta di tale nodo.
 */
public class RequestHandler implements Runnable{
	private Socket socket;
	private Node node;
	private Object lookupSync;
	/**
	 * @param socket: la socket che dovr� essere utilizzata per comunicare con il nodo che ha richiesto la connessione
	 */
	public RequestHandler(Socket socket, Node node, Object lookupSync) {
		this.socket = socket;
		this.node = node;
		this.lookupSync = lookupSync;
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
			String originalSender = json.getString("original_sender");
			String currentSender = json.getString("current_sender");
			System.out.println(json.get("op_code") + " from " + currentSender + " to " + node.getNodeIP());

			switch(Command.valueOf((String) json.get("op_code"))) {
			case JOIN:
				try {
					node.createJSON(Command.SUCC_RES, originalSender, originalSender, node.findSuccessor(node.evaluateID(originalSender)).getHostAddress());
				}catch(NullPointerException e) {
					node.createJSON(Command.SUCC_REQ, originalSender, node.closestPrecedingNode(node.getNodeID()).getHostAddress(), null);
				}
				break;

			case SUCC_REQ:
				if(json.isNull("payload"))
					//node.createJSON(Command.SUCC_RES, originalSender, originalSender, node.findSuccessor(senderID, originalSender, null).getHostAddress());
					try {
						node.createJSON(Command.SUCC_RES, originalSender, originalSender, node.findSuccessor(node.evaluateID(originalSender)).getHostAddress());
					} catch(NullPointerException e) {
						node.createJSON(Command.SUCC_REQ, originalSender, node.closestPrecedingNode(node.evaluateID(originalSender)).getHostAddress(), null);
					}
				else {
					BigInteger entryKey = node.evaluateID(originalSender).add(BigInteger.valueOf((long) 2).pow(json.getInt("payload") - 1)).mod(node.getRingDimension());
					try {
						node.createJSON(Command.FIX_RES, originalSender, originalSender, node.findSuccessor(entryKey).getHostAddress() + "@" + json.getInt("payload"));
					} catch (NullPointerException e) {
						node.createJSON(Command.SUCC_REQ, originalSender, node.closestPrecedingNode(entryKey).getHostAddress(), json.getString("payload"));
					}
				}
				break;

			case SUCC_RES:
				node.setSuccessor(InetAddress.getByName(json.getString("payload")));
				synchronized(node) {
					node.notifyAll();
				}
				break;
				
			case SS_REQ:
				node.createJSON(Command.SS_RES, originalSender, originalSender, node.getSuccessor().getHostAddress());
				break;
				
			case SS_RES:
				node.setSuccSucc(InetAddress.getByName(json.getString("payload")));
				synchronized(node) {
					node.notifyAll();
				}
				break;
				
			case FIX_RES:
				node.setFinger(json.getString("payload"));
				break;
				
			case PRED_REQ:
				try {
					node.createJSON(Command.PRED_RES, originalSender, originalSender, node.getPredecessor().getHostAddress());
				} catch(NullPointerException e) {
					node.createJSON(Command.PRED_RES, originalSender, originalSender, null);
				}
				break;

			case PRED_RES:
				if(!json.isNull("payload"))
					node.setSuccPred(InetAddress.getByName(json.get("payload").toString()));
				else if(node.getSuccessor().equals(InetAddress.getByName(node.getNodeIP())))
					node.setPredecessor(InetAddress.getByName(node.getNodeIP()));

				synchronized(node) {
					node.notifyAll();
				}
				
				break;
				
			case LOOKUP_REQ:
				try {
					node.createJSON(Command.LOOKUP_RES, originalSender, originalSender, node.findSuccessor(node.evaluateID(originalSender)).getHostAddress());
				} catch(NullPointerException e) {
					node.createJSON(Command.LOOKUP_REQ, originalSender, node.closestPrecedingNode(node.evaluateID(originalSender)).getHostAddress(), null);
				}
				break;	
			
			case LOOKUP_RES:
				node.setLookupResponse(InetAddress.getByName(node.getNodeIP()));
				
				synchronized(lookupSync) {
					lookupSync.notifyAll();
				}
				
				break;
				
			case NOTIFY:
				node.notify(InetAddress.getByName(originalSender));
				break;
			
			default:
				break;
			}

			in.close();
			socket.close();
		} catch (IOException | NoSuchAlgorithmException | JSONException e) {
			e.printStackTrace();
		}
	}

}
