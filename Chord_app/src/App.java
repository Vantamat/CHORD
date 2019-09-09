import java.io.IOException;
import java.net.InetAddress;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import node.Node;

public class App {
	
	public static void main(String[] args) throws NoSuchAlgorithmException, IOException, InterruptedException {
		Node n1 = new Node();
		boolean t = false;
		int choice = 0;
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
		while(choice != 3) {
			System.out.println("1. print node info\n2. lookup\n3. leave\n");
			choice = Integer.valueOf(stdin.nextLine());
			switch(choice) {
			case 1:
				n1.printNodeInformation();
				break;
			case 2:
				String s;
				System.out.println("Insert a key: ");
				s = stdin.nextLine();
				System.out.println("Hashed key: " + n1.evaluateID(s));
				System.out.println(n1.lookup(s));
				break;
			case 3:
				n1.leave();
				break;
			}
		}
		stdin.close();
		return;
	}
}
