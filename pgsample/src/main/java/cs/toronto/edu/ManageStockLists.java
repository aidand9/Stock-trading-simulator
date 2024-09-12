package cs.toronto.edu;

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.InputMismatchException;
import org.postgresql.util.PSQLException;

public class ManageStockLists {

    public static void createStockList(String username, Statement stmt, Scanner input) {
        while (true) {
            try {
                System.out.print("Enter the visibility (public/private): ");
                String visibility = input.nextLine().toLowerCase();

                // Validate visibility input
                while (!visibility.equals("public") && !visibility.equals("private")) {
                    System.out.print("Invalid input. Enter the visibility (public/private): ");
                    visibility = input.nextLine().toLowerCase();
                }

                // Insert new stock list into the database and return the generated StockListID
                String insertStockList = String.format(
                    "INSERT INTO StockList (Visibility, CreatorUsername) VALUES ('%s', '%s') RETURNING StockListID;",
                    visibility, username);
                ResultSet rs = stmt.executeQuery(insertStockList);

                if (rs.next()) {
                    int stockListId = rs.getInt("StockListID");
                    System.out.println("Stock list created successfully with ID: " + stockListId);
                    addStocksToList(username, stockListId, stmt, input);
                }
                rs.close();
                break;  // Exit the loop if insertion is successful

            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                input.nextLine();  
            } catch (PSQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
        manageStockListsHome(username, stmt, input); 
    }

    public static void addStocksToList(String username, int stockListId, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the symbol of the stock to add (or type 'done' to finish): ");
            String symbol = input.nextLine();
            if (symbol.equalsIgnoreCase("done")) {
                break;
            }

            System.out.print("Enter the number of shares: ");
            try {
                int numberOfShares = input.nextInt();
                input.nextLine();  

                // Check if the stock exists
                String queryStock = String.format("SELECT * FROM Stock WHERE Symbol = '%s';", symbol);
                ResultSet rs = stmt.executeQuery(queryStock);

                if (!rs.next()) {
                    System.out.println("Stock with symbol " + symbol + " does not exist. Please try again.");
                    rs.close();
                    continue;
                }
                rs.close();

                // Check if the stock is already in the stock list
                String queryStockListContains = String.format(
                    "SELECT * FROM StockListContains slc " +
                    "JOIN StockHolding sh ON slc.StockHoldingID = sh.StockHoldingID " +
                    "WHERE slc.StockListID = %d AND sh.Symbol = '%s';", stockListId, symbol);
                rs = stmt.executeQuery(queryStockListContains);

                if (rs.next()) {
                    System.out.println("Stock with symbol " + symbol + " is already in the stock list. Please try again.");
                    rs.close();
                    continue;
                }
                rs.close();

               
                String insertStockHolding = String.format(
                    "INSERT INTO StockHolding (Symbol, NumberOfShares) VALUES ('%s', %d) RETURNING StockHoldingID;", symbol, numberOfShares);
                rs = stmt.executeQuery(insertStockHolding);

                if (rs.next()) {
                    int stockHoldingId = rs.getInt("StockHoldingID");

                    // Link stock holding with stock list
                    String insertStockListContains = String.format(
                        "INSERT INTO StockListContains (StockHoldingID, StockListID) VALUES (%d, %d);",
                        stockHoldingId, stockListId);
                    stmt.executeUpdate(insertStockListContains);

                    System.out.println("Stock " + symbol + " with " + numberOfShares + " shares added to the stock list.");
                }

                rs.close();
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number of shares.");
                input.nextLine(); 
            } catch (PSQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
        manageStockListsHome(username, stmt, input);  
    }

    private static void removeStockFromList(int stockListId, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the symbol of the stock to remove (or type 'done' to finish): ");
            String symbol = input.nextLine();
            if (symbol.equalsIgnoreCase("done")) {
                break;
            }

            try {
               
                String queryStockHolding = String.format(
                    "SELECT sh.StockHoldingID FROM StockHolding sh " +
                    "JOIN StockListContains slc ON sh.StockHoldingID = slc.StockHoldingID " +
                    "WHERE slc.StockListID = %d AND sh.Symbol = '%s';", stockListId, symbol);
                ResultSet rs = stmt.executeQuery(queryStockHolding);

                if (!rs.next()) {
                    System.out.println("Stock with symbol " + symbol + " not found in the stock list.");
                    rs.close();
                    continue;
                }
                int stockHoldingId = rs.getInt("StockHoldingID");
                rs.close();

                // Delete the stock holding
                String deleteStockHolding = String.format(
                    "DELETE FROM StockHolding WHERE StockHoldingID = %d;", stockHoldingId);
                stmt.executeUpdate(deleteStockHolding);

                // Delete the stock list contains entry
                String deleteStockListContains = String.format(
                    "DELETE FROM StockListContains WHERE StockHoldingID = %d AND StockListID = %d;", stockHoldingId, stockListId);
                stmt.executeUpdate(deleteStockListContains);

                System.out.println("Stock " + symbol + " removed from the stock list.");

            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
    }

    private static void updateStockShares(int stockListId, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the symbol of the stock to update (or type 'done' to finish): ");
            String symbol = input.nextLine();
            if (symbol.equalsIgnoreCase("done")) {
                break;
            }

            System.out.print("Enter the new number of shares: ");
            try {
                int numberOfShares = input.nextInt();
                input.nextLine();  

                
                String queryStockHolding = String.format(
                    "SELECT sh.StockHoldingID FROM StockHolding sh " +
                    "JOIN StockListContains slc ON sh.StockHoldingID = slc.StockHoldingID " +
                    "WHERE slc.StockListID = %d AND sh.Symbol = '%s';", stockListId, symbol);
                ResultSet rs = stmt.executeQuery(queryStockHolding);

                if (!rs.next()) {
                    System.out.println("Stock with symbol " + symbol + " not found in the stock list.");
                    rs.close();
                    continue;
                }
                int stockHoldingId = rs.getInt("StockHoldingID");
                rs.close();

                // Update the number of shares
                String updateStockHolding = String.format(
                    "UPDATE StockHolding SET NumberOfShares = %d WHERE StockHoldingID = %d;",
                    numberOfShares, stockHoldingId);
                stmt.executeUpdate(updateStockHolding);

                System.out.println("Updated stock " + symbol + " with " + numberOfShares + " shares in the stock list.");

            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number of shares.");
                input.nextLine();  
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
    }

    public static void editStockList(String username, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the ID of the stock list to edit (or type 'done' to go back): ");
            String inputStr = input.nextLine();
            if (inputStr.equalsIgnoreCase("done")) {
                manageStockListsHome(username, stmt, input);
                return;
            }

            try {
                int stockListId = Integer.parseInt(inputStr);

                String queryCheckStockList = String.format(
                    "SELECT * FROM StockList WHERE StockListID = %d AND CreatorUsername = '%s';", stockListId, username);
                ResultSet rs = stmt.executeQuery(queryCheckStockList);

                if (!rs.next()) {
                    System.out.println("Stock list with ID " + stockListId + " does not exist or you are not the creator.");
                    rs.close();
                    continue;
                }
                rs.close();

                while (true) {
                    System.out.println("1 - Add a stock to the list");
                    System.out.println("2 - Remove a stock from the list");
                    System.out.println("3 - Update the number of shares for a stock");
                    System.out.println("4 - Back to previous page");
                    System.out.print("Enter your choice: ");
                    String choice = input.nextLine();

                    switch (choice) {
                        case "1":
                            addStocksToList(username, stockListId, stmt, input);
                            break;
                        case "2":
                            removeStockFromList(stockListId, stmt, input);
                            break;
                        case "3":
                            updateStockShares(stockListId, stmt, input);
                            break;
                        case "4":
                            manageStockListsHome(username, stmt, input);
                            return;
                        default:
                            System.out.println("Invalid choice. Please try again.");
                    }
                }

            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
    }

    public static void viewReviews(String username, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the ID of the stock list to view reviews (or type 'done' to go back): ");
            String inputStr = input.nextLine();
            if (inputStr.equalsIgnoreCase("done")) {
                manageStockListsHome(username, stmt, input);
                return;
            }

            try {
                int stockListId = Integer.parseInt(inputStr);

                String queryCheckStockList = String.format(
                    "SELECT * FROM StockList WHERE StockListID = %d AND CreatorUsername = '%s';", stockListId, username);
                ResultSet rs = stmt.executeQuery(queryCheckStockList);

                if (!rs.next()) {
                    System.out.println("Stock list with ID " + stockListId + " does not exist or you are not the creator.");
                    rs.close();
                    continue;
                }
                rs.close();

                String queryReviews = String.format("SELECT WriterUsername, Body FROM Reviews WHERE StockListID = %d;", stockListId);
                rs = stmt.executeQuery(queryReviews);

                System.out.println("\nReviews for Stock List ID: " + stockListId);
                boolean hasReviews = false;
                while (rs.next()) {
                    hasReviews = true;
                    String writerUsername = rs.getString("WriterUsername");
                    String body = rs.getString("Body");
                    System.out.println("Reviewer: " + writerUsername);
                    System.out.println("Review: " + body);
                    System.out.println("-----------");
                }
                if (!hasReviews) {
                    System.out.println("No reviews found for this Stock List.");
                }

                rs.close();
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
        manageStockListsHome(username, stmt, input);  
    }

    public static void deleteStockList(String username, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the ID of the stock list to delete (or type 'done' to go back): ");
            String inputStr = input.nextLine();
            if (inputStr.equalsIgnoreCase("done")) {
                manageStockListsHome(username, stmt, input);
                return;
            }

            try {
                int stockListId = Integer.parseInt(inputStr);

                String queryCheckStockList = String.format(
                    "SELECT * FROM StockList WHERE StockListID = %d AND CreatorUsername = '%s';", stockListId, username);
                ResultSet rs = stmt.executeQuery(queryCheckStockList);

                if (!rs.next()) {
                    System.out.println("Stock list with ID " + stockListId + " does not exist or you are not the creator.");
                    rs.close();
                    continue;
                }
                rs.close();

                String deleteStockListContains = String.format("DELETE FROM StockListContains WHERE StockListID = %d;", stockListId);
                stmt.executeUpdate(deleteStockListContains);

                String deleteCanSee = String.format("DELETE FROM CanSeeStockList WHERE StockListID = %d;", stockListId);
                stmt.executeUpdate(deleteCanSee);

                String deleteReviews = String.format("DELETE FROM Reviews WHERE StockListID = %d;", stockListId);
                stmt.executeUpdate(deleteReviews);

                String deleteStockList = String.format("DELETE FROM StockList WHERE StockListID = %d;", stockListId);
                stmt.executeUpdate(deleteStockList);

                System.out.println("Stock list with ID " + stockListId + " deleted successfully.");
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
            } catch (PSQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;

            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
        manageStockListsHome(username, stmt, input);  
    }

    public static void changeVisibility(String username, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the ID of the stock list to change visibility (or type 'done' to go back): ");
            String inputStr = input.nextLine();
            if (inputStr.equalsIgnoreCase("done")) {
                manageStockListsHome(username, stmt, input);
                return;
            }

            try {
                int stockListId = Integer.parseInt(inputStr);

                String queryCheckStockList = String.format(
                    "SELECT * FROM StockList WHERE StockListID = %d AND CreatorUsername = '%s';", stockListId, username);
                ResultSet rs = stmt.executeQuery(queryCheckStockList);

                if (!rs.next()) {
                    System.out.println("Stock list with ID " + stockListId + " does not exist or you are not the creator.");
                    rs.close();
                    continue;
                }
                rs.close();

                System.out.print("Enter the new visibility (public/private): ");
                String visibility = input.nextLine().toLowerCase();

                while (!visibility.equals("public") && !visibility.equals("private")) {
                    System.out.print("Invalid input. Enter the visibility (public/private): ");
                    visibility = input.nextLine().toLowerCase();
                }

                String updateVisibility = String.format("UPDATE StockList SET Visibility = '%s' WHERE StockListID = %d;", visibility, stockListId);
                stmt.executeUpdate(updateVisibility);
                System.out.println("Visibility of stock list with ID " + stockListId + " updated to " + visibility + ".");
                break;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
            } catch (PSQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
        manageStockListsHome(username, stmt, input);  
    }

    public static void shareStockList(String username, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the ID of the stock list to share (or type 'done' to go back): ");
            String inputStr = input.nextLine();
            if (inputStr.equalsIgnoreCase("done")) {
                manageStockListsHome(username, stmt, input);
                return;
            }
            int stockListId;
            try {
                stockListId = Integer.parseInt(inputStr);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
                continue;
            }

            try {
                // Check if the stock list exists and is accessible
                String queryCheckStockList = String.format(
                    "SELECT * FROM StockList WHERE StockListID = %d AND CreatorUsername = '%s';", stockListId, username);
                ResultSet rs = stmt.executeQuery(queryCheckStockList);

                if (!rs.next()) {
                    System.out.println("Stock list with ID " + stockListId + " does not exist or you do not have permission to share it.");
                    rs.close();
                    continue;
                }
                rs.close();

                System.out.print("Enter the username of the user to share with: ");
                String shareUsername = input.nextLine();

                // Check if the user exists
                String queryCheckUser = String.format("SELECT * FROM Users WHERE Username = '%s';", shareUsername);
                rs = stmt.executeQuery(queryCheckUser);

                if (!rs.next()) {
                    System.out.println("User with username " + shareUsername + " does not exist.");
                    rs.close();
                    continue;
                }
                rs.close();

                // Share the stock list with the user
                String insertCanSee = String.format(
                    "INSERT INTO CanSeeStockList (Username, StockListID) VALUES ('%s', %d);", shareUsername, stockListId);
                stmt.executeUpdate(insertCanSee);
                System.out.println("Stock list with ID " + stockListId + " shared successfully with " + shareUsername + ".");
                break;  // Exit the loop if a valid ID was entered

            } catch (PSQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
        manageStockListsHome(username, stmt, input);  
    }

    public static void manageStockListsHome(String username, Statement stmt, Scanner input) {
        System.out.println("\n1 - Create a new Stock List");
        System.out.println("2 - Edit an existing Stock List");
        System.out.println("3 - View Reviews for your Stock Lists");
        System.out.println("4 - Delete a Stock List");
        System.out.println("5 - Change Stock List Visibility");
        System.out.println("6 - Share a Stock List");
        System.out.println("7 - Back to previous page");
        System.out.print("What would you like to do?: ");
        String choice = input.nextLine();

        switch (choice) {
            case "1":
                createStockList(username, stmt, input);
                break;
            case "2":
                editStockList(username, stmt, input);
                break;
            case "3":
                viewReviews(username, stmt, input);
                break;
            case "4":
                deleteStockList(username, stmt, input);
                break;
            case "5":
                changeVisibility(username, stmt, input);
                break;
            case "6":
                shareStockList(username, stmt, input);
                break;
            case "7":
                Main.navigationPage(username, stmt, input);
                break;
            default:
                System.out.println("Invalid choice. Please try again.");
                manageStockListsHome(username, stmt, input);
        }
    }
}
