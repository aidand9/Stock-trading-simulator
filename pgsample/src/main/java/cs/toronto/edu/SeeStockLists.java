package cs.toronto.edu;

import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.text.ParseException;
import java.text.SimpleDateFormat;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.CategoryTextAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;



public class SeeStockLists {

    public static void viewStockLists(String username, Statement stmt, Scanner input, int num) {
        try {
            String query;
            if(num == 0){

                query = String.format(
                    "SELECT DISTINCT sl.StockListID, sl.Visibility, sl.CreatorUsername " +
                    "FROM StockList sl " +
                    "LEFT JOIN CanSeeStockList cs ON sl.StockListID = cs.StockListID " +
                    "WHERE sl.Visibility = 'public' OR sl.CreatorUsername = '%s' OR cs.Username = '%s';",
                    username, username);
                    System.out.println("\nAvailable Stock Lists:");
            }
            else{
                query = String.format("SELECT DISTINCT sl.StockListID, sl.Visibility, sl.CreatorUsername " +
                    "FROM StockList sl " + "WHERE sl.CreatorUsername = '%s';", username);
                    System.out.println("\nYour Stock Lists:");
            }
            
            ResultSet rs = stmt.executeQuery(query);

            System.out.println("=========================================");
            boolean hasStockLists = false;
            while (rs.next()) {
                hasStockLists = true;
                int stockListId = rs.getInt("StockListID");
                String visibility = rs.getString("Visibility");
                String creatorUsername = rs.getString("CreatorUsername");
        

                System.out.println("Stock List ID: " + stockListId);
                System.out.println("Visibility: " + visibility);
                System.out.println("Creator: " + creatorUsername);
                System.out.println("-----------");
            }
            if (!hasStockLists) {
                System.out.println("No stock lists available.");
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        seeStockListsHome(username, stmt, input); 
    }

    public static void reviewStockList(String username, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the ID of the stock list to review (or type 'done' to go back): ");
            String inputStr = input.nextLine();
            if (inputStr.equalsIgnoreCase("done")) {
                seeStockListsHome(username, stmt, input);
                return;
            }

            try {
                int stockListId = Integer.parseInt(inputStr);

                // Check if the stock list exists and is accessible
                String queryCheckStockList = String.format(
                    "SELECT * FROM StockList sl " +
                    "LEFT JOIN CanSeeStockList cs ON sl.StockListID = cs.StockListID " +
                    "WHERE sl.StockListID = %d AND (sl.Visibility = 'public' OR sl.CreatorUsername = '%s' OR cs.Username = '%s');",
                    stockListId, username, username);
                ResultSet rs = stmt.executeQuery(queryCheckStockList);

                if (!rs.next()) {
                    System.out.println("Stock list with ID " + stockListId + " does not exist or is not accessible to you.");
                    rs.close();
                    continue;
                }
                rs.close();

                
                System.out.print("Enter your review (max 4000 characters): ");
                String review = input.nextLine();

                // Check if the review already exists
                String queryCheckReview = String.format(
                    "SELECT * FROM Reviews WHERE StockListID = %d AND WriterUsername = '%s';", stockListId, username);
                rs = stmt.executeQuery(queryCheckReview);

                if (rs.next()) {
                    // Update existing review
                    String updateReview = String.format(
                        "UPDATE Reviews SET Body = '%s' WHERE StockListID = %d AND WriterUsername = '%s';",
                        review, stockListId, username);
                    stmt.executeUpdate(updateReview);
                    System.out.println("Review updated successfully.");
                } else {
                    // Insert new review
                    String insertReview = String.format(
                        "INSERT INTO Reviews (StockListID, WriterUsername, Body) VALUES (%d, '%s', '%s');",
                        stockListId, username, review);
                    stmt.executeUpdate(insertReview);
                    System.out.println("Review added successfully.");
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
        seeStockListsHome(username, stmt, input); 
    }

    public static void viewStockListDetails(String username, int stockListId, Statement stmt, Scanner input) {
        try {
            String query = String.format(
                "SELECT sl.StockListID, sl.Visibility, sl.CreatorUsername " +
                "FROM StockList sl " +
                "LEFT JOIN CanSeeStockList cs ON sl.StockListID = cs.StockListID " +
                "WHERE sl.StockListID = %d AND (sl.Visibility = 'public' OR sl.CreatorUsername = '%s' OR cs.Username = '%s');",
                stockListId, username, username);
            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                String visibility = rs.getString("Visibility");
                String creatorUsername = rs.getString("CreatorUsername");


                System.out.println("\nStock List ID: " + stockListId);
                System.out.println("Visibility: " + visibility);
                System.out.println("Creator: " + creatorUsername);


                // Check access for detailed information
                boolean canViewDetails = visibility.equals("public") || creatorUsername.equals(username) || isStockListSharedWithUser(stockListId, username, stmt);

                if (canViewDetails) {
                    // Show stocks only if the user has access
                    System.out.println("\nStocks in this Stock List:");
                    String queryStocks = String.format(
                        "SELECT Symbol, NumberOfShares " +
                        "FROM StockHolding sh " +
                        "JOIN StockListContains slc ON sh.StockHoldingID = slc.StockHoldingID " +
                        "WHERE slc.StockListID = %d;", stockListId);
                    ResultSet rsStocks = stmt.executeQuery(queryStocks);

                    boolean hasStocks = false;
                    while (rsStocks.next()) {
                        hasStocks = true;
                        String symbol = rsStocks.getString("Symbol");
                        int numberOfShares = rsStocks.getInt("NumberOfShares");

                        System.out.println("Symbol: " + symbol + ", Number of Shares: " + numberOfShares);
                    }
                    if (!hasStocks) {
                        System.out.println("No stocks found in this Stock List.");
                    }

                    rsStocks.close();
                    System.out.println("\nReviews for this Stock List:");
                    String queryReviews;
                    if(isStockListSharedWithUser(stockListId, username, stmt) && visibility.equals("private")){
                        queryReviews = String.format(
                        "SELECT WriterUsername, Body FROM Reviews WHERE StockListID = %d AND WriterUsername = '%s';", stockListId, username);
                    }else{
                        queryReviews = String.format(
                        "SELECT WriterUsername, Body FROM Reviews WHERE StockListID = %d;", stockListId);
                    }
                    
                    ResultSet rsReviews = stmt.executeQuery(queryReviews);

                    boolean hasReviews = false;
                    while (rsReviews.next()) {
                        hasReviews = true;
                        String writerUsername = rsReviews.getString("WriterUsername");
                        String body = rsReviews.getString("Body");

                        System.out.println("Reviewer: " + writerUsername);
                        System.out.println("Review: " + body);
                        System.out.println("-----------");
                    }
                    if (!hasReviews) {
                        System.out.println("No reviews found for this Stock List.");
                    }

                    rsReviews.close();
                } else {
                    System.out.println("This stock list is private and not accessible to you.");
                }

            } else {
                System.out.println("Stock list with ID " + stockListId + " does not exist or is not accessible to you.");
            }

            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        seeStockListsHome(username, stmt, input);  
    }

    private static boolean isStockListSharedWithUser(int stockListId, String username, Statement stmt) throws SQLException {
        String query = String.format("SELECT * FROM CanSeeStockList WHERE StockListID = %d AND Username = '%s';", stockListId, username);
        ResultSet rs = stmt.executeQuery(query);
        boolean isShared = rs.next();
        rs.close();
        return isShared;
    }

    public static void deleteReview(String username, Statement stmt, Scanner input) {
        while (true) {
            System.out.print("Enter the ID of the stock list to delete the review from (or type 'done' to go back): ");
            String inputStr = input.nextLine();
            if (inputStr.equalsIgnoreCase("done")) {
                seeStockListsHome(username, stmt, input);
                return;
            }

            try {
                int stockListId = Integer.parseInt(inputStr);

                // Check if the stock list exists and is accessible to the user
                String queryCheckStockList = String.format(
                    "SELECT * FROM StockList sl LEFT JOIN CanSeeStockList cs ON sl.StockListID = cs.StockListID " +
                    "WHERE sl.StockListID = %d AND (sl.Visibility = 'public' OR sl.CreatorUsername = '%s' OR cs.Username = '%s' OR EXISTS (SELECT 1 FROM Reviews WHERE StockListID = %d AND WriterUsername = '%s'));",
                    stockListId, username, username, stockListId, username);
                ResultSet rs = stmt.executeQuery(queryCheckStockList);

                if (!rs.next()) {
                    System.out.println("Stock list with ID " + stockListId + " does not exist or is not accessible to you.");
                    rs.close();
                    continue;
                }
                rs.close();

                System.out.print("Enter the username of the review to delete (or type 'done' to go back): ");
                String reviewUsername = input.nextLine();
                if (reviewUsername.equalsIgnoreCase("done")) {
                    continue;
                }

                // Check if the review exists and is deletable by the current user
                String queryCheckReview = String.format(
                    "SELECT * FROM Reviews WHERE StockListID = %d AND WriterUsername = '%s';", stockListId, reviewUsername);
                rs = stmt.executeQuery(queryCheckReview);

                if (!rs.next()) {
                    System.out.println("Review by user " + reviewUsername + " does not exist for this stock list.");
                    rs.close();
                    continue;
                }
                rs.close();

                String deleteReview = String.format(
                    "DELETE FROM Reviews WHERE StockListID = %d AND WriterUsername = '%s' AND (WriterUsername = '%s' OR '%s' = (SELECT CreatorUsername FROM StockList WHERE StockListID = %d));",
                    stockListId, reviewUsername, username, username, stockListId);
                int rowsAffected = stmt.executeUpdate(deleteReview);

                if (rowsAffected > 0) {
                    System.out.println("Review by user " + reviewUsername + " deleted successfully.");
                } else {
                    System.out.println("You do not have permission to delete this review.");
                }

                break;  // Exit the loop if a valid ID was entered
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                return;
            }
        }
        seeStockListsHome(username, stmt, input);  
    }

    public static void viewHistoricalPrices(String username, Statement stmt, Scanner input) {
        System.out.print("Enter the symbol of the stock: ");
        String stockSymbol = input.nextLine().toUpperCase();
    
        System.out.print("Enter the start date (YYYY-MM-DD): ");
        String startDateStr = input.nextLine();
    
        // Validate the start date
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        
        try {
           dateFormat.parse(startDateStr);
        } catch (ParseException e) {
            System.out.println("Invalid date format. Please enter the date in YYYY-MM-DD format.");
            viewHistoricalPrices(username, stmt, input);
            return;
        }
    
        int intervalChoice = 0;
        while (true) {
            System.out.println("Select the interval:");
            System.out.println("1 - A week");
            System.out.println("2 - A month");
            System.out.println("3 - A quarter");
            System.out.println("4 - A year");
            System.out.println("5 - Five years");
            System.out.print("Enter your choice: ");
            if (input.hasNextInt()) {
                intervalChoice = input.nextInt();
                input.nextLine();
                if (intervalChoice >= 1 && intervalChoice <= 5) {
                    break;
                } else {
                    System.out.println("Invalid choice. Please enter a number between 1 and 5.");
                }
            } else {
                System.out.println("Invalid choice. Please enter a valid number.");
                input.next(); 
            }
        }
    
        String intervalQuery;
        switch (intervalChoice) {
            case 1:
                intervalQuery = "7 days";
                break;
            case 2:
                intervalQuery = "1 month";
                break;
            case 3:
                intervalQuery = "3 months";
                break;
            case 4:
                intervalQuery = "1 year";
                break;
            case 5:
                intervalQuery = "5 years";
                break;
            default:
                intervalQuery = "1 month"; 
        }
    
        try {
            String query = String.format(
                "SELECT Date, Close FROM DailyStockListing WHERE Symbol = '%s' AND Date >= (DATE '%s' - INTERVAL '%s') ORDER BY Date;",
                stockSymbol, startDateStr, intervalQuery);
            ResultSet rs = stmt.executeQuery(query);
    
            List<String> dates = new ArrayList<>();
            List<Double> prices = new ArrayList<>();
            while (rs.next()) {
                dates.add(rs.getString("Date"));
                prices.add(rs.getDouble("Close"));
            }
            rs.close();
    
            if (prices.isEmpty()) {
                System.out.println("No historical data available for the given interval for " + stockSymbol);
            } else {
                generateGraph(stockSymbol, dates, prices, "Historical Prices");
            }
    
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
    
    }
public static void predictFuturePrice(String username, Statement stmt, Scanner input) {
    System.out.print("Enter the symbol of the stock: ");
    String stockSymbol = input.nextLine().toUpperCase();

    int intervalChoice = 0;
    while (true) {
        System.out.println("Select the interval for prediction:");
        System.out.println("1 - A week");
        System.out.println("2 - A month");
        System.out.println("3 - A quarter");
        System.out.println("4 - A year");
        System.out.println("5 - Five years");
        System.out.print("Enter your choice: ");
        if (input.hasNextInt()) {
            intervalChoice = input.nextInt();
            input.nextLine();
            if (intervalChoice >= 1 && intervalChoice <= 5) {
                break;
            } else {
                System.out.println("Invalid choice. Please enter a number between 1 and 5.");
            }
        } else {
            System.out.println("Invalid choice. Please enter a valid number.");
            input.next(); 
        }
    }

    // Fetch historical prices
    List<Double> historicalPrices = new ArrayList<>();
    List<String> historicalDates = new ArrayList<>();
    try {
        String query = String.format("SELECT Date, Close FROM DailyStockListing WHERE Symbol = '%s' ORDER BY Date DESC LIMIT 30", stockSymbol);
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            historicalPrices.add(rs.getDouble("Close"));
            historicalDates.add(rs.getString("Date"));
        }
        rs.close();

        if (historicalPrices.isEmpty()) {
            System.out.println("No historical data available for the stock symbol: " + stockSymbol);
        } else {
            // Calculate the moving average 
            double sum = 0.0;
            for (double price : historicalPrices) {
                sum += price;
            }
            double movingAverage = sum / historicalPrices.size();
            int daysInFuture;
            switch (intervalChoice) {
                case 1:
                    daysInFuture = 7;
                    break;
                case 2:
                    daysInFuture = 30;
                    break;
                case 3:
                    daysInFuture = 90;
                    break;
                case 4:
                    daysInFuture = 365;
                    break;
                case 5:
                    daysInFuture = 1825;
                    break;
                default:
                    daysInFuture = 30; 
            }

            // Generate data for the future prices
            List<String> futureDates = new ArrayList<>();
            List<Double> futurePrices = new ArrayList<>();
            LocalDate lastDate = LocalDate.parse(historicalDates.get(0));
            for (int i = 1; i <= daysInFuture; i++) {
                LocalDate futureDate = lastDate.plusDays(i);
                futureDates.add(futureDate.format(DateTimeFormatter.ISO_DATE));
                double predictedPrice = movingAverage + (i * (historicalPrices.get(0) - movingAverage) / daysInFuture); 
                futurePrices.add(predictedPrice);
            }

          

            generateGraph(stockSymbol, futureDates, futurePrices, "Predicted Prices");
        }

    } catch (SQLException e) {
        e.printStackTrace();
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
}

private static void generateGraph(String stockSymbol, List<String> dates, List<Double> prices, String title) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    for (int i = 0; i < dates.size(); i++) {
        dataset.addValue(prices.get(i), "Price", dates.get(i));
    }

    // Include the first and last dates in the title
    String fullTitle = title + " for " + stockSymbol + " (" + dates.get(0) + " to " + dates.get(dates.size() - 1) + ")";

    JFreeChart lineChart = ChartFactory.createLineChart(
        fullTitle,
        "Date", "Price in $",
        dataset,
        PlotOrientation.VERTICAL,
        true, true, false);

    CategoryPlot plot = lineChart.getCategoryPlot();
    NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

    CategoryAxis domainAxis = plot.getDomainAxis();

  
    domainAxis.setTickLabelsVisible(false);
    domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);




    // Label first and last date on the graph
    plot.addAnnotation(new CategoryTextAnnotation(dates.get(0), dates.get(0), prices.get(0)));
    plot.addAnnotation(new CategoryTextAnnotation(dates.get(dates.size() - 1), dates.get(dates.size() - 1), prices.get(prices.size() - 1)));  

    int width = 640; 
    int height = 480; 
    File lineChartFile = new File(stockSymbol + "_" + title.replace(" ", "_") + ".jpeg");
    try {
        ChartUtilities.saveChartAsJPEG(lineChartFile, lineChart, width, height);
        System.out.println("Graph generated: " + lineChartFile.getAbsolutePath());
    } catch (IOException e) {
        e.printStackTrace();
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
}

public static void viewStockBetas(String username, int stockListId, Statement stmt, Scanner input) {
    try {
        String query = String.format(
            "SELECT sl.StockListID, sl.Visibility, sl.CreatorUsername " +
            "FROM StockList sl " +
            "LEFT JOIN CanSeeStockList cs ON sl.StockListID = cs.StockListID " +
            "WHERE sl.StockListID = %d AND (sl.Visibility = 'public' OR sl.CreatorUsername = '%s' OR cs.Username = '%s');",
            stockListId, username, username);
        ResultSet rs = stmt.executeQuery(query);

        if (rs.next()) {
            String visibility = rs.getString("Visibility");
            String creatorUsername = rs.getString("CreatorUsername");

            boolean canViewDetails = visibility.equals("public") || creatorUsername.equals(username) || isStockListSharedWithUser(stockListId, username, stmt);

            if (canViewDetails) {
                List<String> symbolsList = getStockSymbolsFromStockList(stockListId, stmt);
                if (symbolsList.isEmpty()) {
                    System.out.println("No stocks found in this stock list.");
                } else {
                    ArrayList<String> symbols = new ArrayList<>(symbolsList);

                    ManagePortfolio.viewStockBetas(symbols, stmt, input);

                }
            }
        }
        rs.close();
    } catch (SQLException e) {
        e.printStackTrace();
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    seeStockListsHome(username, stmt, input); // Go back to the previous page
}
    

public static void viewCovMatrix(String username, int stockListId, Statement stmt, Scanner input) {
    try {
        String query = String.format(
            "SELECT sl.StockListID, sl.Visibility, sl.CreatorUsername " +
            "FROM StockList sl " +
            "LEFT JOIN CanSeeStockList cs ON sl.StockListID = cs.StockListID " +
            "WHERE sl.StockListID = %d AND (sl.Visibility = 'public' OR sl.CreatorUsername = '%s' OR cs.Username = '%s');",
            stockListId, username, username);
        ResultSet rs = stmt.executeQuery(query);

        if (rs.next()) {
            String visibility = rs.getString("Visibility");
            String creatorUsername = rs.getString("CreatorUsername");

            boolean canViewDetails = visibility.equals("public") || creatorUsername.equals(username) || isStockListSharedWithUser(stockListId, username, stmt);

            if (canViewDetails) {
                List<String> symbolsList = getStockSymbolsFromStockList(stockListId, stmt);
                if (symbolsList.isEmpty()) {
                    System.out.println("No stocks found in this stock list.");
                } else {
                    ArrayList<String> symbols = new ArrayList<>(symbolsList);
            
                    ManagePortfolio.viewCovMatrix(symbols, stmt, input);
                    
                }
            } else {
                System.out.println("This stock list is private and not accessible to you.");
            }
        } else {
            System.out.println("Stock list with ID " + stockListId + " does not exist or is not accessible to you.");
        }

        rs.close();
    } catch (SQLException e) {
        e.printStackTrace();
        System.err.println(e.getClass().getName() + ": " + e.getMessage());
    }
    seeStockListsHome(username, stmt, input); // Go back to the previous page
}

private static List<String> getStockSymbolsFromStockList(int stockListId, Statement stmt) throws SQLException {
    List<String> symbols = new ArrayList<>();
    String query = String.format("SELECT Symbol FROM StockHolding sh JOIN StockListContains slc ON sh.StockHoldingID = slc.StockHoldingID WHERE slc.StockListID = %d;", stockListId);
    ResultSet rs = stmt.executeQuery(query);
    while (rs.next()) {
        symbols.add(rs.getString("Symbol"));
    }
    rs.close();
    return symbols;
}
    


public static void seeStockListsHome(String username, Statement stmt, Scanner input) {
    System.out.println("\n1 - View All Stock Lists");
    System.out.println("2 - View your Stock Lists");
    System.out.println("3 - Review a Stock List");
    System.out.println("4 - View Stock List Details");
    System.out.println("5 - Delete a Review");
    System.out.println("6 - View Historical Prices of a Stock");
    System.out.println("7 - Predict Future Stock Price");
    System.out.println("8 - View Stock Betas");
    System.out.println("9 - View Covariance Matrix");
    System.out.println("10 - Back to previous page");
    System.out.print("What would you like to do?: ");
    String choice = input.nextLine();

    switch (choice) {
        case "1":
            viewStockLists(username, stmt, input, 0);
            break;

        case "2":
            viewStockLists(username, stmt, input, 1);
            break;
        case "3":
            reviewStockList(username, stmt, input);
            break;
        case "4":
            try {
                System.out.print("Enter the ID of the stock list to view details (or type 'done' to go back): ");
                String inputStr = input.nextLine();
                if (inputStr.equalsIgnoreCase("done")) {
                    seeStockListsHome(username, stmt, input);
                    return;
                }
                int stockListId = Integer.parseInt(inputStr);
                viewStockListDetails(username, stockListId, stmt, input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
                seeStockListsHome(username, stmt, input);
            }
            break;
        case "5":
            deleteReview(username, stmt, input);
            break;
        case "6":
            viewHistoricalPrices(username, stmt, input);
            seeStockListsHome(username, stmt, input);
            break;
        case "7":
            predictFuturePrice(username, stmt, input);
            seeStockListsHome(username, stmt, input);
            break;
        case "8":
            try {
                System.out.print("Enter the ID of the stock list to view betas (or type 'done' to go back): ");
                String inputStr = input.nextLine();
                if (inputStr.equalsIgnoreCase("done")) {
                    seeStockListsHome(username, stmt, input);
                    return;
                }
                int stockListId = Integer.parseInt(inputStr);
                viewStockBetas(username, stockListId, stmt, input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
                seeStockListsHome(username, stmt, input);
            }
            break;
        case "9":
            try {
                System.out.print("Enter the ID of the stock list to view covariance matrix (or type 'done' to go back): ");
                String inputStr = input.nextLine();
                if (inputStr.equalsIgnoreCase("done")) {
                    seeStockListsHome(username, stmt, input);
                    return;
                }
                int stockListId = Integer.parseInt(inputStr);
                viewCovMatrix(username, stockListId, stmt, input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid stock list ID.");
                seeStockListsHome(username, stmt, input);
            }
            break;
        case "10":
            Main.navigationPage(username, stmt, input);
            break;
        default:
            System.out.println("Invalid choice. Please try again.");
            seeStockListsHome(username, stmt, input);
    }
    
}
}