import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import node.Node;

public class App {
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InterruptedException {
		System.out.println("Digit an option:");
		Node n1 = new Node();
		boolean t = false;
		int choice = 0;
		Scanner stdin = new Scanner(System.in);
		while(!t) {
			System.out.println("1. CREATE a new ring\n2. JOIN an existing ring");
			choice = Integer.valueOf(stdin.nextLine());
			System.out.println("New ring created");
			switch(choice) {
			case 1:
				t = n1.create();
				break;
			case 2:
				System.out.println("Insert the IP to join:");
				t = n1.join(InetAddress.getByName(stdin.nextLine()));
				System.out.println("Ring joined successfully");				
				break;
			}
			System.out.println("__________________________________________________________________________");
		}
		while(choice != 4) {
			System.out.println("\nDigit an option:");
			System.out.println("1. PRINT your node information\n2. PRINT your finger table\n3. LOOKUP for a key in the ring\n4. LEAVE the ring");
			choice = Integer.valueOf(stdin.nextLine());
			switch(choice) {
			case 1:
				n1.printNodeInformation();
				break;
			case 2:
				n1.printFingerTable();
				break;
			case 3:
				String s;
				System.out.println("Insert a key: ");
				s = stdin.nextLine();
				System.out.println("Hashed key:\t" + n1.evaluateID(s));
				System.out.println("Key owner:\t" + n1.lookup(s));
				break;
			case 4:
				System.out.println("Leaving the ring");
				n1.leave();
				break;
			}
			System.out.println("_________________________________________________________________________________");
		}
		stdin.close();
		System.exit(0);
	}
}
