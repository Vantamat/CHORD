import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import node.Node;

public class App {
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InterruptedException {
		Node n1 = new Node();
		boolean t = false;
		int choice;
		Scanner stdin = new Scanner(System.in);
		while(!t) {
			System.out.println("1. create\n2. join\n");
			choice = Integer.valueOf(stdin.nextLine());
			switch(choice) {
			case 1:
				t = n1.create();
				break;
			case 2:
				System.out.println("IP to join: ");
				t = n1.join(InetAddress.getByName(stdin.nextLine()));
				break;
			}
		}
		n1.lookup("ciao");
	}

}
