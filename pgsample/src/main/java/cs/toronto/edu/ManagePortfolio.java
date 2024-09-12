package cs.toronto.edu;

import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.sql.ResultSet;

public class ManagePortfolio {

    // Allows the user to record a new daily stock listing
    private static void recordNewDailyStockListing(String username, Statement stmt, Scanner input) {
        System.out.print("Please enter the symbol of the stock you would like to add info for: (Just press enter to go back): ");
        String symbol = input.nextLine();

        if(symbol.equals("")) {
            managePorfolioHome(username, stmt, input);
            return;
        }

        if(!doesStockExist(symbol, stmt)) {
            String sqlUpdate = String.format("INSERT INTO stock(symbol) " +
                                         "VALUES(\'%s\');", symbol);
            Main.SQL_insert(sqlUpdate, stmt);
        }

        System.out.print("Please enter the date of the new listing: ");
        String date = input.nextLine();

        if(isThereDailyListing(symbol, date, stmt) != -1) {
            System.out.println("This stock already has a listing for this date!");
            recordNewDailyStockListing(username, stmt, input);
            return;
        }

        System.out.print("Please enter the open value of the new listing: ");
        float open = input.nextFloat();
        System.out.print("Please enter the high value of the new listing: ");
        float high = input.nextFloat();
        System.out.print("Please enter the low value of the new listing: ");
        float low = input.nextFloat();
        System.out.print("Please enter the close value of the new listing: ");
        float close = input.nextFloat();
        System.out.print("Please enter the volume value of the new listing: ");
        long volume = input.nextLong();

        String sqlUpdate = String.format("INSERT INTO dailystocklisting(symbol, date, open, high, low, close, volume) " +
                                         "VALUES(\'%s\', \'%s\', %f, %f, %f, %f, %d);", symbol, date, open, high, low, close, volume);
        Main.SQL_insert(sqlUpdate, stmt);

        System.out.println("New listing successfully added!");
        input.nextLine();
        recordNewDailyStockListing(username, stmt, input);

    }

    // Shows the transaction history of the portfolio with id PORTFOLIOID
    private static void viewTransactionHistory(String portfolio, int portfolioID, String username, Statement stmt, Scanner input) {
        String query = String.format("SELECT symbol, numberofshares, date, type " +
                                     "FROM transaction " +
                                     "WHERE portfolioid = %d;", portfolioID);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(!rs.next()) {
                System.out.println("\n*******************************");
                System.out.println("*    No Transactions Found    *");
                System.out.println("*******************************");
            }
            else {

                ArrayList<String> symbols = new ArrayList<>();
                ArrayList<Integer> shares = new ArrayList<>();
                ArrayList<String> dates = new ArrayList<>();
                ArrayList<String> types = new ArrayList<>();

                do {
                    String date = rs.getString("date");
                    String symbol = rs.getString("symbol");
                    String type = rs.getString("type");
                    int share = rs.getInt("numberofshares");
                
                    dates.add(date);
                    symbols.add(symbol);
                    shares.add(share);
                    types.add(type);

                } while(rs.next());

                for(int i = 0; i < symbols.size(); i++) {
                    System.out.println("*****************************************");
                    System.out.println("*\tStock: " + symbols.get(i) + "\t\t\t*");
                    System.out.println("*\tShares: " + shares.get(i) + "\t\t\t*");
                    System.out.println("*\tCost: $" + isThereDailyListing(symbols.get(i), dates.get(i), stmt) * shares.get(i) + "\t\t\t*");
                    System.out.println("*\tType: " + types.get(i) + "\t\t\t*");
                    System.out.println("*\tDate: " + dates.get(i) + "\t\t*");
                    System.out.println("*****************************************");
                }

                rs.close();
            }
            System.out.print("Press enter to return to previous page ");
            input.nextLine();
            viewPortfolio(username, portfolio, stmt, input);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    // Deletes the portfolio with the name PORTFOLIO
    private static void deletePortfolio(String username, String portfolio, Statement stmt, Scanner input) {
        System.out.print("Are you sure you want to delete your portfolio named " + portfolio + "? [y/n]: ");
        String choice = input.nextLine();

        if(!choice.equalsIgnoreCase("y")) {
            managePorfolioHome(username, stmt, input);
            return;
        }

        String query = String.format("SELECT portfolioid " +
                                     "FROM portfolio " +
                                     "WHERE portfolioname = \'%s\' AND username = \'%s\';", portfolio, username);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(rs.next()) {
                int portfolioID = rs.getInt("portfolioid");

                // Delete all transactions stored for this portfolio
                String sqlDelete = String.format("DELETE FROM transaction " +
                                                 "WHERE portfolioid = %d ", portfolioID);
                Main.SQL_insert(sqlDelete, stmt);

                // Delete all related entries in portfoliocontains table and return the stockholdingIDs
                sqlDelete = String.format("DELETE FROM portfoliocontains " +
                                          "WHERE portfolioid = %d " +
                                          "RETURNING stockholdingid;", portfolioID);
                rs = stmt.executeQuery(sqlDelete);

                // Delete all related entries in stockholding table
                ArrayList<Integer> shIDs = new ArrayList<>();
                while(rs.next()) {
                    shIDs.add(rs.getInt("stockholdingid"));
                }

                for(int i = 0; i < shIDs.size(); i++) {
                    sqlDelete = String.format("DELETE FROM stockholding " +
                                              "WHERE stockholdingid = %d;", shIDs.get(i));
                    Main.SQL_insert(sqlDelete, stmt);
                }

                // Delete portfolio entry in portfolio table
                sqlDelete = String.format("DELETE FROM portfolio " +
                                          "WHERE portfolioid = %d;", portfolioID);
                Main.SQL_insert(sqlDelete, stmt);

                System.out.println("Portfolio successfully deleted!");

            }
            else {
                System.out.print("Could not find a portfolio named " + portfolio);
                input.nextLine();
                managePorfolioHome(username, stmt, input);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

    }

    // Returns true if a stock with the symbol SYMBOL exists in the database
    private static boolean doesStockExist(String symbol, Statement stmt) {

        String query = String.format("SELECT * FROM stock WHERE symbol = \'%s\';", symbol);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            return rs.next();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        return false;
    }

    // Checks if there is an entry in dailystocklisting with SYMBOL and DATE. IF there is, this function returns the close price for that entry. Otherwise,
    // it returns -1;
    private static float isThereDailyListing(String symbol, String date, Statement stmt) {

        String query = String.format("SELECT close " +
                                     "FROM dailystocklisting " +
                                     "WHERE symbol = \'%s\' AND date = \'%s\';", symbol, date);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(rs.next())
                return (float)rs.getDouble("close");
            else
                return -1;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
            return 0;
        }
    }

    // Allows user to record a sale into their portfolio
    private static void recordSale(String portfolio, int portfolioID, String username, Statement stmt, Scanner input) {
        System.out.print("Please enter the symbol of the stock you sold: ");
        String symbol = input.nextLine();
        System.out.print("How many shares did you sell?: ");
        int sharesSold = input.nextInt();
        input.nextLine();
        System.out.print("Please enter the date of the transaction [yyyy-mm-dd]: ");
        String date = input.nextLine();

        float close = isThereDailyListing(symbol, date, stmt);

        if (close == -1) {
            System.out.println("Could not find a listing for this stock for the inputted date. \nPlease enter new information for this date if you wish to make this transaction.");
            input.nextLine();
            viewPortfolio(username, portfolio, stmt, input);
            return;
        }
        
        double cost = sharesSold * close;

        String query = String.format("SELECT sh.stockholdingid, sh.numberofshares " +
                                    "FROM portfoliocontains pc, stockholding sh " +
                                    "WHERE pc.portfolioid = %d AND pc.stockholdingid = sh.stockholdingid AND sh.symbol = \'%s\';", portfolioID, symbol);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(rs.next()) {
                // Add new shares bought to already existing stock holding in portfolio
                int stockHoldingID = rs.getInt("stockholdingid");
                int sharesHeld = rs.getInt("numberofshares");

                if(sharesHeld < sharesSold) {
                    System.out.println("Transaction could not be recorded. The number of shares being sold is greater than the amount held in this portfolio.");
                    input.nextLine();
                    recordTransaction(portfolio, portfolioID, username, stmt, input);
                    return;
                }
                else if(sharesHeld == sharesSold) {
                    // Delete the stock holding since all shares have been sold.
                    String sqlUpdate = String.format("DELETE FROM portfoliocontains WHERE stockholdingid = %d;" +
                                                     "DELETE FROM stockholding WHERE stockholdingid = %d;", stockHoldingID, stockHoldingID);
                    Main.SQL_insert(sqlUpdate, stmt);
                    System.out.println("Stock holding removed from portfolio!");
                }
                else {
                    // Remove the shares from the stock holding
                    String sqlUpdate = String.format("UPDATE stockholding " +
                                                     "SET numberofshares = numberofshares - %d " +
                                                     "WHERE stockholdingid = %d;", sharesSold, stockHoldingID);
                    Main.SQL_insert(sqlUpdate, stmt);
                    DecimalFormat df = new DecimalFormat("0.00");
                    System.out.print("Shares successfully sold! $" + df.format(cost) + " has been added to this portfolio's balance!");
                }

                // Update the portfolio balance
                String sqlUpdate = String.format("UPDATE portfolio " +
                                          "SET balance = balance + %f " +
                                          "WHERE portfolioid = %d", (float)cost, portfolioID);
                Main.SQL_insert(sqlUpdate, stmt);

                // Record the purchase into the transaction table.
                sqlUpdate = String.format("INSERT INTO transaction(portfolioid, numberofshares, symbol, date, type) " +
                                          "VALUES(%d, %d, \'%s\', \'%s\', \'SALE\');", portfolioID, sharesSold, symbol, date);
                Main.SQL_insert(sqlUpdate, stmt);

                input.nextLine();
                recordTransaction(portfolio, portfolioID, username, stmt, input);
            }
            else {
                System.out.println("This portfolio does not contain any stocks with that symbol. Please try again.");
                input.nextLine();
                recordTransaction(portfolio, portfolioID, username, stmt, input);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

    }
    
    // Allows user to record a purchase into their portfolio
    private static void recordPurchase(String portfolio, int portfolioID, String username, Statement stmt, Scanner input) {
        System.out.print("Please enter the symbol of the stock you purchased: ");
        String symbol = input.nextLine();
        System.out.print("How many shares did you purchase?: ");
        int shares = input.nextInt();
        input.nextLine();
        System.out.print("Please enter the date of the transaction [yyyy-mm-dd]: ");
        String date = input.nextLine();

        float close = isThereDailyListing(symbol, date, stmt);

        if(!doesStockExist(symbol, stmt)) {
            System.out.println("Could not find any data on this stock!");
            input.nextLine();
            recordTransaction(portfolio, portfolioID, username, stmt, input);
            return;
        }
        else if(close == -1) {
            System.out.println("Could not find a listing for this stock for the inputted date. \nPlease enter new information for this date if you wish to make this transaction.");
            viewPortfolio(username, portfolio, stmt, input);
            return;
        }

        double cost = shares * close;

        String query = String.format("SELECT pc.stockholdingid " +
                                    "FROM portfoliocontains pc, stockholding sh " +
                                    "WHERE pc.portfolioid = %d AND pc.stockholdingid = sh.stockholdingid AND sh.symbol = \'%s\';", portfolioID, symbol);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(rs.next()) {
                // Add new shares bought to already existing stock holding in portfolio
                int stockHoldingID = rs.getInt("stockholdingid");
                String sqlUpdate = String.format("UPDATE stockholding " +
                                                 "SET numberofshares = numberofshares + %d " +
                                                 "WHERE stockholdingid = %d;", shares, stockHoldingID);
                Main.SQL_insert(sqlUpdate, stmt);
                System.out.print("New shares successfully purchased!");
            }
            else {
                // Create new stock holding and add it to portfolio
                String sqlUpdate = String.format("INSERT INTO stockholding(symbol, numberofshares) " +
                                                 "VALUES(\'%s\', %d) " +
                                                 "RETURNING stockholdingid;", symbol, shares);
                rs = stmt.executeQuery(sqlUpdate);
                rs.next();
                int stockholdingID = rs.getInt("stockholdingid");
                sqlUpdate = String.format("INSERT INTO portfoliocontains(portfolioid, stockholdingid) " +
                                          "VALUES(\'%s\', %d);", portfolioID, stockholdingID);
                Main.SQL_insert(sqlUpdate, stmt);
                System.out.print("New shares successfully purchased!");
            }

            // Update the portfolio balance
            String sqlUpdate = String.format("UPDATE portfolio " +
                                             "SET balance = balance - %f " +
                                             "WHERE portfolioid = %d", (float)cost, portfolioID);
            Main.SQL_insert(sqlUpdate, stmt);

            // Record the purchase into the transaction table.
            sqlUpdate = String.format("INSERT INTO transaction(portfolioid, numberofshares, symbol, date, type) " +
                                      "VALUES(%d, %d, \'%s\', \'%s\', \'PURCHASE\');", portfolioID, shares, symbol,  date);
            Main.SQL_insert(sqlUpdate, stmt);

            input.nextLine();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
        viewPortfolio(username, portfolio, stmt, input);
    }

    private static void recordTransaction(String portfolio, int portfolioID, String username, Statement stmt, Scanner input) {
        System.out.print("Would you like to record a purchase or a sale of stocks? [p/s] (Just press enter to go back to previous page): ");
        String choice = input.nextLine();

        if(choice.equalsIgnoreCase("p")) {
            recordPurchase(portfolio, portfolioID, username, stmt, input);
        }
        else if(choice.equalsIgnoreCase("s")) {
            recordSale(portfolio, portfolioID, username, stmt, input);
        }
        else {
            viewPortfolio(username, portfolio, stmt, input);
        }
    }

    // Allows user to transfer funds from one portfolio to another
    private static double portfolioCashTransfer(String portfolio, int portfolioID, String username, Statement stmt, Scanner input) {
        System.out.print("Please enter the name of the portfolio you would like to transfer from (Just press Enter to go back): ");
        String transfer = input.nextLine();

        if(transfer.equals(portfolio)) {
            System.out.println("You cannot transfer from the same portfolio.");
            return 0.0;
        }
        else if(transfer.equals("")) {
            return 0.0;
        }
        else {
            double change = 0.0;

            String query = String.format("SELECT portfolioid, balance " +
                                         "FROM portfolio " +
                                         "WHERE username = \'%s\' AND portfolioname = \'%s\';", username, transfer);
            ResultSet rs = Main.SQL_select(query, stmt);

            try {
                if(rs.next()) {
                    double balance = rs.getDouble("balance");
                    int transferID = rs.getInt("portfolioid");
                    System.out.print("How much would you like to transfer from this portfolio (in $)?: ");
                    change = input.nextFloat();

                    if(change > balance) {
                        DecimalFormat df = new DecimalFormat("0.00");
                        System.out.println("This portfolio does not have enough funds for this transfer. It only has $" + df.format(balance));
                        return 0.0;
                    }
                    else {
                        // Take funds out of portfolio being transferred from
                        String sqlUpdate = String.format("UPDATE portfolio " +
                                                         "SET balance = balance - %f " +
                                                         "WHERE portfolioid = %d;", change, transferID);
                        Main.SQL_insert(sqlUpdate, stmt);
                        return change;
                    }
                }
                else {
                    System.out.println("You do not have any portfolios by that name.");
                    return 0.0;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(1);
            }

            return change;
        }
    }

    // Allows for deposit/withdrawal from the cash account belonging to the portfolio with id PORTFOLIOID
    private static void depositOrWithdrawal(String portfolio, int portfolioID, String username, Statement stmt, Scanner input) {
        System.out.print("Would you like to make a deposit or a withdrawal [d/w] (Just press Enter to go back): ");
        String choice = input.nextLine();

        float change = 0;

        if(choice.equalsIgnoreCase("d")) {
            System.out.print("Would you like to deposit from an external bank account or transfer from another portfolio? [b/p]: ");
            choice = input.nextLine();
            if(choice.equalsIgnoreCase("b")) {
                System.out.print("How much would you like to deposit (in $)?: ");
                change = input.nextFloat();
            }
            else {
                change = (float)portfolioCashTransfer(portfolio, portfolioID, username, stmt, input);
                if (change == 0.0)
                    depositOrWithdrawal(portfolio, portfolioID, username, stmt, input);
            }
             
        }
        else if (choice.equalsIgnoreCase("w")) {
            System.out.print("How much would you like to withdraw (in $)?: ");
            change = input.nextFloat() * -1; 
        }
        else {
            managePorfolioHome(username, stmt, input);
            return;
        }

        String sqlUpdate = String.format("UPDATE portfolio " +
                                         "SET balance = balance + %f " +
                                         "WHERE portfolioid = \'%d\'" +
                                         "RETURNING balance", change, portfolioID);
        try {
            ResultSet rs = stmt.executeQuery(sqlUpdate);
            rs.next();
            double balance = rs.getDouble("balance");
            DecimalFormat df = new DecimalFormat("0.00");
            System.out.println("Success! Your new balance is $" + df.format(balance));
            input.nextLine();
            viewPortfolio(username, portfolio, stmt, input);
        } 
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    // Returns the most recent close price of the stock SYMBOL
    private static Double getMostRecentClosePrice(String symbol, Statement stmt) {
        String query = String.format("SELECT close " + 
                                     "FROM dailystocklisting " + 
                                     "WHERE symbol = \'%s\' AND (symbol, date) IN " +
                                        "(SELECT symbol, MAX(date) " +
                                        "FROM dailystocklisting " +
                                        "GROUP BY symbol);", symbol);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            rs.next();
            Double close = rs.getDouble("close");
            return close;
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        return 0.0;
    }

    // Finds the estimated market value of a Stock Holding by mutliplying it's most recent closing price by
    // the number of shares held.
    private static Double getStockHoldingMarketValue(int stockHoldingID, String username, Statement stmt) {
        Double balance = 0.0;

        String query = String.format("SELECT symbol, numberofshares " + 
                                     "FROM stockholding " +
                                     "WHERE stockholdingid = %d;", stockHoldingID);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {

            ArrayList<String> symbols = new ArrayList<>();
            ArrayList<Integer> shares = new ArrayList<>();

            while(rs.next()) {
                String symbol = rs.getString("symbol");
                int share = rs.getInt("numberofshares");
                symbols.add(symbol);
                shares.add(share);
            }

            for(int i = 0; i < symbols.size(); i++) {
                Double close = getMostRecentClosePrice(symbols.get(i), stmt);
                balance += close * shares.get(i);
            }
            
            rs.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        return balance;
    }

    // Finds the market value of the portfolio by adding up the market value of all stock holdings in the portfolio
    private static Double getPortfilioMarketValue(String portfolio, String username, Statement stmt) {
        
        String query = String.format("SELECT stockholdingid " + 
                                     "FROM portfolio, portfoliocontains " +
                                     "WHERE username = \'%s\' AND portfolioname = \'%s\' AND portfolio.portfolioid = portfoliocontains.portfolioid;", username, portfolio);
        ResultSet rs = Main.SQL_select(query, stmt);
        Double balance = 0.0;

        try {
            ArrayList<Integer> ids = new ArrayList<Integer>();

            while(rs.next()) {
                int stockHoldingID = rs.getInt("stockholdingid");
                ids.add(stockHoldingID);
            }

            for(int i = 0; i < ids.size(); i++) {
                balance += getStockHoldingMarketValue((int)ids.get(i), username, stmt);
            }

            rs.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        return balance;
    }

    /*
    // Calculates the covariance of the two arrays MARKETRETURNS and STOCKRETURNS
    private static double calcucateCOV(ArrayList<Double> array1, ArrayList<Double> array2) {

        int size = Math.min(array1.size(), array2.size());
        
        // Calculate the covariance
        double cov = 0;
        for (int i = 0; i < size; i++)
            cov = cov + array1.get(i) * array2.get(i);
        return cov / (size - 1);
    }
    */

    private static boolean isThereData(String symbol, String startDate, String endDate, Statement stmt) {

        String query;
        if(symbol.equals("market")) {
            query = String.format("SELECT * " +
                                         "FROM dailystocklisting " +
                                         "WHERE date >= \'%s\' AND date <= \'%s\';", startDate, endDate);
        }
        else {
            query = String.format("SELECT * " +
                                         "FROM dailystocklisting " +
                                         "WHERE symbol = \'%s\' AND date >= \'%s\' AND date <= \'%s\';", symbol, startDate, endDate);
        }
        
        try {
            ResultSet rs = Main.SQL_select(query, stmt);
            return rs.next();
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        return false;

    }

    // Shows the Beta Coefficient of every stock in the SYMBOLS list
    public static void viewStockBetas(ArrayList<String> symbols, Statement stmt, Scanner input) {

        System.out.print("Please enter the start date for the beta calculation [yyyy-mm-dd]: ");
        String startDate = input.nextLine();
        System.out.print("Please enter the end date for the beta calculation [yyyy-mm-dd]: ");
        String endDate = input.nextLine();

        if (!isThereData("market", startDate, endDate, stmt)) {
            System.out.println("No data found in this time range.");
            System.out.print("Please press enter to go back to previous page.");
            input.nextLine();
            return;
        }

        DecimalFormat df1 = new DecimalFormat("0.00000000");
        DecimalFormat df2 = new DecimalFormat("0.00");

        String query = "SELECT VARIANCE(change2) as var " +
                    "FROM " +
                    "    ( " +
                    "    SELECT date, (LEAD(sum) OVER (ORDER BY date) - sum) / sum AS change2  " +
                    "    FROM  " +
                    "        ( " +
                    "        SELECT SUM(close), date  " +
                    "        FROM dailystocklisting  " +
                    "        GROUP BY date " +
                    "        )  " +
                    "    WHERE date >= \'" + startDate + "\' AND date <= \'" + endDate + "\' " +
                    "    )";
        ResultSet rs = Main.SQL_select(query, stmt);
        double var = 1;
        try {
            rs.next();
            var = rs.getDouble("var");
        } 
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

        for(int i = 0; i < symbols.size(); i++) {

            if (!isThereData(symbols.get(i), startDate, endDate, stmt)) {
                System.out.println(symbols.get(i) + ": No data found.");
                continue;
            }

            query = "SELECT SUM(x * y) / (COUNT(x) - 1) as cov " +
                    "FROM " +
                    "( " +
                    "SELECT change1 - " +
                    "    ( " +
                    "    SELECT AVG(change1) " +
                    "    FROM " +
                    "        ( " +
                    "        SELECT date, (LEAD(close) OVER (ORDER BY date) - close) / close AS change1 " +
                    "        FROM dailystocklisting " +
                    "        WHERE symbol = \'" + symbols.get(i) +"\' AND date >= \'" + startDate + "\' AND date <= \'" + endDate + "\'" +
                    "        ) " +
                    "    ) as x " +
                    ", change2 - " +
                    "    ( " +
                    "    SELECT AVG(change2) " +
                    "    FROM " +
                    "        ( " +
                    "        SELECT date, (LEAD(sum) OVER (ORDER BY date) - sum) / sum AS change2 " +
                    "        FROM " +
                    "            ( " +
                    "            SELECT SUM(close), date " +
                    "            FROM dailystocklisting " +
                    "            GROUP BY date " +
                    "            ) " +
                    "        WHERE date >= \'" + startDate + "\' AND date <= \'" + endDate + "\' " +
                    "        ) " +
                    "    ) as y " +
                    "FROM " +
                    "    (" +
                    "    SELECT date, (LEAD(close) OVER (ORDER BY date) - close) / close AS change1 " +
                    "    FROM dailystocklisting " +
                    "    WHERE symbol = \'" + symbols.get(i) + "\' AND date >= \'" + startDate + "\' AND date <= \'" + endDate + "\' " +
                    "    ) as c1 " +
                    "JOIN " +
                    "    ( " +
                    "    SELECT date, (LEAD(sum) OVER (ORDER BY date) - sum) / sum AS change2 " +
                    "    FROM " +
                    "        ( " +
                    "        SELECT SUM(close), date " +
                    "        FROM dailystocklisting " +
                    "        GROUP BY date " +
                    "        ) " +
                    "    WHERE date >= \'" + startDate + "\' AND date <= \'" + endDate + "\' " +
                    "    ) as c2 " +
                    "ON c1.date = c2.date " +
                    ");";

            rs = Main.SQL_select(query, stmt);
            double cov = 0;

            try {
                rs.next();
                cov = rs.getDouble("cov");
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(1);
            }

            double beta = cov / var;

            System.out.println(symbols.get(i) + ": COV = " + df1.format(cov) + ", Beta = " + df2.format(beta));
        }

        System.out.print("Please press enter to go back to previous page.");
        input.nextLine();
    }

    public static void viewCovMatrix(ArrayList<String> symbols, Statement stmt, Scanner input) {

        String[][] matrix = new String[symbols.size()][symbols.size()];
        System.out.print("Please enter the start date for the beta calculation [yyyy-mm-dd]: ");
        String startDate = input.nextLine();
        System.out.print("Please enter the end date for the beta calculation [yyyy-mm-dd]: ");
        String endDate = input.nextLine();

        DecimalFormat df = new DecimalFormat("0.00000000");

        for(int i = 0; i < symbols.size(); i++) {
            for(int j = i; j < symbols.size(); j++) {
                String stock1 = symbols.get(i);
                String stock2 = symbols.get(j);


                String query =  "SELECT SUM(x * y) / (COUNT(x) - 1) as cov " +
                                "FROM " +
                                "( " +
                                "SELECT change1 - " +
                                "    ( " +
                                "    SELECT AVG(change1) " +
                                "    FROM " +
                                "        ( " +
                                "        SELECT date, (LEAD(close) OVER (ORDER BY date) - close) / close AS change1 " +
                                "        FROM dailystocklisting " +
                                "        WHERE symbol = \'" + stock1 +"\' AND date >= \'" + startDate + "\' AND date <= \'" + endDate + "\'" +
                                "        ) " +
                                "    ) as x " +
                                ", change2 - " +
                                "    ( " +
                                "    SELECT AVG(change2) " +
                                "    FROM " +
                                "        ( " +
                                "        SELECT date, (LEAD(close) OVER (ORDER BY date) - close) / close AS change2 " +
                                "        FROM dailystocklisting " +
                                "        WHERE symbol = \'" + stock2 +"\' AND date >= \'" + startDate + "\' AND date <= \'" + endDate + "\'" +
                                "        ) " +
                                "    ) as y " +
                                "FROM " +
                                "    (" +
                                "    SELECT date, (LEAD(close) OVER (ORDER BY date) - close) / close AS change1 " +
                                "    FROM dailystocklisting " +
                                "    WHERE symbol = \'" + stock1 + "\' AND date >= \'" + startDate + "\' AND date <= \'" + endDate + "\' " +
                                "    ) as c1 " +
                                "JOIN " +
                                "    (" +
                                "    SELECT date, (LEAD(close) OVER (ORDER BY date) - close) / close AS change2 " +
                                "    FROM dailystocklisting " +
                                "    WHERE symbol = \'" + stock2 + "\' AND date >= \'" + startDate + "\' AND date <= \'" + endDate + "\' " +
                                "    ) as c2 " +
                                "ON c1.date = c2.date " +
                                ");";

                ResultSet rs = Main.SQL_select(query, stmt);
                double cov = 0;
    
                try {
                    rs.next();
                    cov = rs.getDouble("cov");
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.err.println(e.getClass().getName() + ": " + e.getMessage());
                    System.exit(1);
                }

                matrix[i][j] = df.format(cov);
                matrix[j][i] = df.format(cov);

            }
        }

        System.out.print("\t");
        for(int i = 0; i < symbols.size(); i++) {
            System.out.print(symbols.get(i) + "\t\t");
        }
        System.out.println();

        for(int i = 0; i < symbols.size(); i++) {
            System.out.print(symbols.get(i) + "\t");
            for(int j = 0; j < symbols.size(); j++) {
                System.out.print(matrix[i][j] + "\t");
            }
            System.out.println();
        }

        //System.out.println(Arrays.deepToString(matrix).replace("], ", "]\n").replace("[", "").replace("]", "\t"));
        System.out.print("Press enter to return to previous page.");
        input.nextLine();
    }

    // Shows a view of all the stock holdings in the portfolio with id PORTFOLIOID. Shows the stock symbol, the number of shares,
    // and the estimated market value.
    private static void viewPortfolioStockHoldings(String portfolio, int portfolioID, String username, Statement stmt, Scanner input) {
        String query = String.format("SELECT symbol, numberofshares, pc.stockholdingid " +
                                     "FROM portfoliocontains pc, stockholding sh " +
                                     "WHERE pc.portfolioid = %d AND pc.stockholdingid = sh.stockholdingid;", portfolioID);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(!rs.next()) {
                System.out.println("\n*******************************");
                System.out.println("*   No Stock Holdings Found   *");
                System.out.println("*******************************");
                System.out.print("\nPress enter to return to previous page");
                input.nextLine();
                viewPortfolio(username, portfolio, stmt, input);
                return;
            }
            else {

                ArrayList<Integer> ids = new ArrayList<>();
                ArrayList<String> symbols = new ArrayList<>();
                ArrayList<Integer> shares = new ArrayList<>();

                do {
                    int stockHoldingID = rs.getInt("stockholdingid");
                    String symbol = rs.getString("symbol");
                    int share = rs.getInt("numberofshares");

                    ids.add(stockHoldingID);
                    symbols.add(symbol);
                    shares.add(share);
                } while(rs.next());

                for(int i = 0; i < ids.size(); i++) {
                    double marketValue = getStockHoldingMarketValue(ids.get(i), username, stmt);
                    
                    System.out.println("\n\t\t" + symbols.get(i) + "\t\t");
                    System.out.println("*****************************************");
                    System.out.println("*\tShares: " + shares.get(i) + "\t\t\t*");
                    System.out.println("*\tEstimated Market Value: " + marketValue + "\t*");
                    System.out.println("*****************************************");
                }

                System.out.println("1 - View Beta Coefficients and COV of Stocks");
                System.out.println("2 - View COV matrix of Stocks");
                System.out.println("3 - View Historic Data of Stock");
                System.out.println("4 - View Future Predictions for Stock");
                System.out.println("5 - Return to previous page");
                System.out.print("What would you like to do?: ");
                String choice = input.nextLine();
                rs.close();

                switch (choice) {
                    case "1":
                        viewStockBetas(symbols, stmt, input);
                        viewPortfolioStockHoldings(portfolio, portfolioID, username, stmt, input);
                        break;
                    case "2":
                        viewCovMatrix(symbols, stmt, input);
                        viewPortfolioStockHoldings(portfolio, portfolioID, username, stmt, input);
                        break;
                    case "3":
                        SeeStockLists.viewHistoricalPrices(username, stmt, input);
                        viewPortfolioStockHoldings(portfolio, portfolioID, username, stmt, input);
                        break;
                    case "4":
                        SeeStockLists.predictFuturePrice(username, stmt, input);
                        viewPortfolioStockHoldings(portfolio, portfolioID, username, stmt, input);
                        break;
                    default:
                        viewPortfolio(username, portfolio, stmt, input);
                        break;
                }
                    

            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    // Creates a new portfolio, and returns the name 
    private static String createNewPortfolio(String username, Statement stmt, Scanner input) {
        System.out.print("What would you like to name your new portfolio?: ");
        String name = input.nextLine();

        String query = String.format("SELECT * " +
                                     "FROM portfolio " + 
                                     "WHERE username = \'%s\' AND portfolioname = \'%s\'", username, name);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(rs.next()) {
                System.out.println("You already have a portfolio by this name. Please choose a new name.");
                return createNewPortfolio(username, stmt, input);
            }
            else {
                Date date = new Date();
                String sqlInsert = String.format("INSERT INTO portfolio(portfolioname, creationdate, username, balance) " + 
                                                        "VALUES(\'%s\', \'%s\', \'%s\', 0);", name, date, username);
                Main.SQL_insert(sqlInsert, stmt);
                System.out.println("Successfully created portfolio " + name);
                return name;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
        return "";
    }
    
    // Shows the details of portfolio with the name PORTFOLIO
    private static void viewPortfolio(String username, String portfolio, Statement stmt, Scanner input) {
        String query = String.format("SELECT * " + 
                                    "FROM portfolio " + 
                                    "WHERE username = \'%s\' AND portfolioname = \'%s\';", username, portfolio);
        ResultSet rs = Main.SQL_select(query, stmt);
    
        try {
            if (rs.next()) {

                Date creationDate = rs.getDate("creationdate");
                Double balance = rs.getDouble("balance");
                int portfolioID = rs.getInt("portfolioid");
                Double marketValue = getPortfilioMarketValue(portfolio, username, stmt);
                DecimalFormat df = new DecimalFormat("0.00");

                System.out.println("\n\t" + portfolio + "\t\t\t");
                System.out.println("*****************************************");
                System.out.println("\tCreated: " + creationDate + "\t\t");
                System.out.println("\tBalance: $" + df.format(balance) + "\t\t\t");
                System.out.println("\tEstimated Market Value: " + marketValue + "\t");
                System.out.println("*****************************************");

                System.out.println("\n1 - Record a transaction");
                System.out.println("2 - Make a deposit/withdrawal");
                System.out.println("3 - View Stock Holdings");
                System.out.println("4 - View Transaction History");
                System.out.println("5 - Back to previous page");
                System.out.print("What would you like to do: ");
                String choice = input.nextLine();

                switch(choice) {
                    case "1":
                        recordTransaction(portfolio, portfolioID, username, stmt, input);
                        break;
                    case "2":
                        depositOrWithdrawal(portfolio, portfolioID, username, stmt, input);
                        break;
                    case "3":
                        viewPortfolioStockHoldings(portfolio, portfolioID, username, stmt, input);
                        break;
                    case "4":
                        viewTransactionHistory(portfolio, portfolioID, username, stmt, input);
                        break;
                    case "5":
                        managePorfolioHome(username, stmt, input);
                }
            }
            else {
                System.out.println("Could not find any portfolio by that name. Please try again.");
                managePorfolioHome(username, stmt, input);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
    }

    public static void managePorfolioHome(String username, Statement stmt, Scanner input) {        

        String query = String.format("SELECT portfolioname, balance " +
                                     "FROM portfolio " +
                                     "WHERE username = \'%s\'", username);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {

            if(!rs.next()) {
                System.out.println("\n***************************");
                System.out.println("*   No Portfolios Found   *");
                System.out.println("***************************");
            }
            else {
                DecimalFormat df = new DecimalFormat("0.00");
                do {
                    double balance = rs.getDouble("balance");
                    String name = rs.getString("portfolioname");
                    System.out.println("\n*********************************");
                    System.out.println("\t" + name + "\t\t");
                    System.out.println("\tBalance: $" + df.format(balance) + "\t\t");
                    System.out.println("*********************************");
                } while(rs.next());

                rs.close();
            }

            System.out.println("\n1 - Create a new Portfolio");
            System.out.println("2 - Examine one of your Portfolios");
            System.out.println("3 - Delete one of your Portfolios");
            System.out.println("4 - Record new daily stock information");
            System.out.println("5 - Back to previous page");
            System.out.print("What would you like to do?: ");
            String choice = input.nextLine();

            String portfolio = "";
            switch (choice) {
                case "1":
                    portfolio = createNewPortfolio(username, stmt, input);
                    viewPortfolio(username, portfolio, stmt, input);
                    break;
                case "2":
                    System.out.print("Please enter the name of the porfolio you would like to examine: ");
                    portfolio = input.nextLine();
                    viewPortfolio(username, portfolio, stmt, input);
                    break;
                case "3":
                    System.out.print("Please enter the name of the porfolio you would like to delete: ");
                    portfolio = input.nextLine();
                    deletePortfolio(username, portfolio, stmt, input);
                    managePorfolioHome(username, stmt, input);
                    break;
                case "4":
                    recordNewDailyStockListing(username, stmt, input);
                    break;
                default:
                    Main.navigationPage(username, stmt, input);
                    return;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }

    }

}
