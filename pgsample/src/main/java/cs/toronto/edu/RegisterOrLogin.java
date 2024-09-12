package cs.toronto.edu;

import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Scanner;

public class RegisterOrLogin {

    private static String Register(Statement stmt, Scanner input) {
        System.out.print("Please choose a username: ");
        String username = input.nextLine();

        ResultSet rs = Main.SQL_select("SELECT username FROM users WHERE username = \'" + username + "\'", stmt);

        try {
            if (rs.next()) {
                System.out.println("Username already taken. Plese choose a different username.");
                username = Register(stmt, input);
            }

            System.out.print("Please choose a password: ");
            String password = input.nextLine();

            Main.SQL_insert("INSERT INTO users (username, password) VALUES (\'" + username + "\', \'" + password + "\');", stmt);
            System.out.println("Registration Successful!");
            rs.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        return username;
    }

    private static String Login(Statement stmt, Scanner input) {
        System.out.print("Please enter your username (Press Enter without typing to return to previous page): ");
        String username = input.nextLine();
        System.out.print("Please enter your password: ");
        String password = input.nextLine();
        
        ResultSet rs = Main.SQL_select("SELECT username, password FROM users WHERE username = \'" + username + "\'", stmt);
        
        if (username.equals(""))
             return registerOrLogin(stmt, input);
        else {
            try {
                if(rs.next()) {
                    if (rs.getString("password").equals(password)) {
                        System.out.println("Login Successful!");
                    }
                    else {
                        System.out.println("Incorrect password. Please try again.\n\n");
                        username = Login(stmt, input);
                    }
                }
                else {
                    System.out.println("User not found. Please try again.\n");
                    username = Login(stmt, input);
                }
                rs.close();
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(1);
            }
            return username;
        }
    }

    // Directs user to either registartion or login
    public static String registerOrLogin(Statement stmt, Scanner input) {
		System.out.print("Would you like to login or register? [l/r]: ");
		String choice = input.nextLine();

		if(choice.equals("r") || choice.equals("R")) {
			return Register(stmt, input);
		}
		else {
			return Login(stmt, input);
		}
	}
    
}
