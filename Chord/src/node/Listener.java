package node;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Listener implements Runnable{
	private Socket socket;
	
	public Listener(Socket socket) {
		this.socket = socket;
	}
	
	@Override
	public void run() {
		try {
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
