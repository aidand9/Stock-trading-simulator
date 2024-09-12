package cs.toronto.edu;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.Scanner;

public class Main {

	public static void SQL_insert(String sqlInsert, Statement stmt) {
		// Create SQL statement to insert a tuple
		try{
			stmt.executeUpdate(sqlInsert);
			//System.out.println("Tuple inserted successfully");
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
		}
	}

	public static ResultSet SQL_select (String query, Statement stmt) {
		// Create SQL statement to query all tuples
		try {
			String sqlSelect = query;
			ResultSet rs = stmt.executeQuery(sqlSelect);
			return rs;
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
			return null;
		}

		/*
		System.out.println("Table testtbl contains the following tuples:\nname \tvalue");
		while (rs.next()) {
			String name = rs.getString("user1");
			int value = rs.getInt("user2");
			System.out.println(name + " \t" + value);
		}
			rs.close();
		*/
	}

	public static void navigationPage(String username, Statement stmt, Scanner input) {
		System.out.println("\n1 - Manage Friends");
		System.out.println("2 - See Stock Lists");
		System.out.println("3 - Manage your Stock Lists");
		System.out.println("4 - Manage your Portfolios");
		System.out.println("5 - Quit");
		System.out.print("What would you like to do?: ");
		String choice = input.nextLine();

		switch(choice) {
			case "1":
				ManageAccount.manageAccountHome(username, stmt, input);
				break;
			case "2":
				SeeStockLists.seeStockListsHome(username, stmt, input);
				break;
			case "3":
				ManageStockLists.manageStockListsHome(username, stmt, input);
				break;
			case "4":
				ManagePortfolio.managePorfolioHome(username, stmt, input);
				break;
			case "5":
				System.out.println("Goodbye!");
				System.exit(0);
				break;
			default:
				System.out.println("Invalid choice. Please try again.");
				navigationPage(username, stmt, input);
		}
	}

	public static void main(String[] args) {
		Connection conn = null;
		Statement stmt = null;
		Scanner input = new Scanner(System.in);

		try {
			// Register the PostgreSQL driver
			Class.forName("org.postgresql.Driver");
			// Connect to the database
			conn = DriverManager.getConnection("jdbc:postgresql://34.123.128.60:5432/c43_project", "postgres", "postgres");
			System.out.println("Opened database successfully");
			// Create a statement object
			stmt = conn.createStatement();

			// Begin Program

			// On a successful login or registration, username will hold the username of the user.
			String username = RegisterOrLogin.registerOrLogin(stmt, input);

			navigationPage(username, stmt, input);

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
		} finally {
			try {
				if (stmt != null) stmt.close();
				if (conn != null) conn.close();
			
				System.out.println("Disconnected from the database");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
