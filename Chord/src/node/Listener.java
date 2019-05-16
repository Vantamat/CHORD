package node;

import java.io.IOException;
import java.net.ServerSocket;


/**
 * Listener continua a stare in ascolto per richieste di connessione provenienti da altri nodi della rete,
 * quando riceve una richiesta crea un nuovo processo per gestire le richieste del nodo (join, stabilize, ...)
 */

public class Listener implements Runnable{
	//in un sistema p2p un nodo è sia client che server, quindi servono sia una serverSocket per accettare connessioni	
	@Override
	public void run() {
		try {
			//TODO-trovare un modo per chiudere la socket, forse attributo e metodo con .clode chiamato alla chiusura del programma
			//eventualmente serverSocket inizializzata in un'altra classe
			ServerSocket serverSocket = new ServerSocket(2345);
			while(true) {
				new RequestsHandler(serverSocket.accept());
			}
			/*
			String line;
			Scanner in = new Scanner(socket.getInputStream());
			
			while (true) {
				line = in.nextLine();
				System.out.println(line + "\n");
				if (line.equals("quit")) {
					break;
				}
			}
			
			in.close();
			socket.close();
			serverSocket.close();*/
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}

}
