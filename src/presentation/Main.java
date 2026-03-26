package presentation;

import business.PCBUS;
import business.UserBUS;
import entity.PC;
import entity.User;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        UserBUS userBUS = new UserBUS();
        PCBUS pcBUS = new PCBUS();

        while (true) {

            System.out.println("======= CYBER MANAGEMENT =======");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("3. Add PC");
            System.out.println("4. Show PC List");
            System.out.println("0. Exit");
            System.out.print("Choose: ");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:

                    System.out.print("Username: ");
                    String username = sc.nextLine();

                    System.out.print("Password: ");
                    String password = sc.nextLine();

                    User user = new User(username, password);

                    userBUS.register(user);

                    break;

                case 2:

                    System.out.print("Username: ");
                    String u = sc.nextLine();

                    System.out.print("Password: ");
                    String p = sc.nextLine();

                    userBUS.login(u, p);

                    break;

                case 3:

                    System.out.print("PC Name: ");
                    String name = sc.nextLine();

                    System.out.print("Status: ");
                    String status = sc.nextLine();

                    System.out.print("Price per hour: ");
                    double price = sc.nextDouble();
                    sc.nextLine();

                    PC pc = new PC(name, status, price);

                    pcBUS.addPC(pc);

                    break;

                case 4:

                    pcBUS.showPC();

                    break;

                case 0:

                    System.out.println("Exit program!");
                    System.exit(0);

                default:

                    System.out.println("Invalid choice!");
            }
        }
    }
}