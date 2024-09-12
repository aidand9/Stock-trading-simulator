package cs.toronto.edu;

import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Scanner;

public class ManageAccount {

    // Returns true if user with username USERNAME exists. Returns false otherwise.
    private static boolean userExists(String username, Statement stmt) {
        boolean exists = false;

        String query = String.format("SELECT * FROM users WHERE username = \'%s\';", username);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            exists = rs.next();
            rs.close();
        } catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
        }

        return exists;
    }

    // Allows user to accept or reject a friend request
    private static void acceptOrRejectRequest(String username, Statement stmt, Scanner input) {
        System.out.print("Enter the name of the user whose friend request you would like to respond to: ");
        String senderUsername = input.nextLine();

        String query = String.format("SELECT * FROM friendrequests WHERE sender=\'%s\' AND receiver=\'%s\';", senderUsername, username);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(rs.next()) {
                System.out.print("Would you like to accept or reject the friend request from this user [a/r]: ");
                String response = input.nextLine();

                // Reject the friend request. The friend request is not deleted, as we need it to verify that at least 5 minutes have passed
                // before the sender can send another one.
                if(response.equals("r") || response.equals("R")) {
                    Timestamp responseTimestamp = new Timestamp(new Date().getTime());
                    
                    String sqlUpdate = String.format("UPDATE friendrequests " + 
                                                     "SET status = \'REJECTED\', responsetimestamp = \'%s\' " + 
                                                     "WHERE sender = \'%s\' AND receiver = \'%s\';", responseTimestamp, senderUsername, username);
                    Main.SQL_insert(sqlUpdate, stmt);
                    System.out.println("Friend request from " + senderUsername + " has been sucessfully rejected!");
                    viewIncomingRequests(username, stmt, input);
                } 
                // Accept the friend request. The friend request is deleted after the acceptance.
                else {
                    String sqlUpdate = String.format("INSERT INTO friends (user1, user2) VALUES (\'%s\', \'%s\');", senderUsername, username);
                    Main.SQL_insert(sqlUpdate, stmt);
                    String sqlDelete = String.format("DELETE FROM friendrequests WHERE sender = \'%s\' AND receiver = \'%s\'", senderUsername, username);
                    Main.SQL_insert(sqlDelete, stmt);
                    System.out.println("Friend request from " + senderUsername + " has been successfully accepted!");
                    viewIncomingRequests(username, stmt, input);
                }
            } else {
                System.out.println("You do not have an incoming friend request from this user.");
                acceptOrRejectRequest(username, stmt, input);
            }
            rs.close();
        }
        catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
        }
    }

    // Cancel an outgoing friend request
    private static void cancelRequest(String username, Statement stmt, Scanner input) {
        System.out.print("Enter the name of the user whose friend request you would like to cancel: ");
        String receiverUsernmae = input.nextLine();

        String query = String.format("SELECT * FROM friendrequests WHERE sender=\'%s\' AND receiver=\'%s\';", username, receiverUsernmae);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(rs.next()) {
                String sqlUpdate = String.format("DELETE FROM friendrequests WHERE sender = \'%s\' AND receiver = \'%s\';", username, receiverUsernmae);
                Main.SQL_insert(sqlUpdate, stmt);
                System.out.println("Friend request to " + receiverUsernmae + " was successfully deleted!");
                viewIncomingRequests(username, stmt, input);
            } else {
                System.out.println("You do not have an outgoing friend request to this user.");
                cancelRequest(username, stmt, input);
            }
            rs.close();
        }
        catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
        }
    }

    // Allows a user to delete a friend from their friends list
    private static void deleteFriend(String username, Statement stmt, Scanner input) {
        System.out.print("Please enter the username of the friend you would like to delete: ");
        String friend = input.nextLine();

        String query = String.format("SELECT * FROM friends " + 
                                     "WHERE (user1 = \'%s\' AND user2 = \'%s\') OR (user1 = \'%s\' AND user2 = \'%s\')", username, friend, friend, username);
        ResultSet rs = Main.SQL_select(query, stmt);

        try {
            if(rs.next()) {
                String sqlUpdate = String.format("DELETE FROM friends " + 
                                                 "WHERE (user1 = \'%s\' AND user2 = \'%s\') OR (user1 = \'%s\' AND user2 = \'%s\')", username, friend, friend, username);
                Main.SQL_insert(sqlUpdate, stmt);

                Date date = new Date();
                Timestamp timestamp = new Timestamp(date.getTime());
                String sqlInsert = String.format("INSERT INTO friendrequests(sender, receiver, timestamp, status, responsetimestamp) " +
                                                 "VALUES(\'%s\', \'%s\', \'%s\', \'REJECTED\', \'%s\');", friend, username, timestamp, timestamp);
                Main.SQL_insert(sqlInsert, stmt);
                
                System.out.println("You are no longer friends with " + friend);
                viewFriendsList(username, stmt, input);
            } else {
                System.out.println("Could not find any friends with the username " + friend);
                deleteFriend(username, stmt, input);
            }
            rs.close();

        } catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
        }
    }

    // Shows user a list of all their friends.
    private static void viewFriendsList(String username, Statement stmt, Scanner input) {
        ResultSet rs = Main.SQL_select("SELECT user1, user2 FROM friends WHERE user1 = \'" + username + "\' OR user2 = \'" + username + "\';", stmt);
        boolean hasFriends = true;

        System.out.println("\n      FRIENDS LIST      ");
        System.out.println("==========================");
        try {
            if(!rs.next()) {
                hasFriends = false;
                System.out.println("\n**************************");
                System.out.println("*    No friends found    *");
                System.out.println("**************************");
            }
            else {

                do {
                    String user1 = rs.getString("user1");
                    String user2 = rs.getString("user2");
                    
                    String friend = "";
                    if(user1.equals(username))
                        friend = user2;
                    else
                        friend = user1;
                    
                    System.out.println("**************************");
                    System.out.println("*\t" + friend + "\t\t*");
                } while(rs.next());

                System.out.println("**************************");
                
            }

            if (hasFriends) {
                System.out.println("\n1 - Delete Friend");
                System.out.println("2 - Back to previous page");
                System.out.print("What would you like to do?: ");
                String choice = input.nextLine();

                if(choice.equals("1")) deleteFriend(username, stmt, input);
                else manageAccountHome(username, stmt, input);
            }
            else {
                System.out.print("Press Enter to return to previous page.");
                input.nextLine();
                manageAccountHome(username, stmt, input);
            }
            rs.close();
         }
        catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
        }
        

    }

    // Shows a list of all incoming friend requests, and offers the option to respond to them.
    private static void viewIncomingRequests(String username, Statement stmt, Scanner input) {
        String query = String.format("SELECT sender, timestamp FROM friendrequests WHERE receiver=\'%s\' AND status='PENDING';", username);
        ResultSet rs = Main.SQL_select(query, stmt);
        boolean hasRequests = false;

        System.out.println("\n         INCOMING FRIEND REQUESTS      ");
        System.out.println("=========================================");

        try {
            if(!rs.next()) {
                System.out.println("\n*****************************************");
                System.out.println("*      No incoming requests found       *");
                System.out.println("*****************************************");
            }
            else {
                hasRequests = true;

                do {
                    String sender = rs.getString("sender");
                    String timestamp = rs.getString("timestamp");
                    
                    System.out.println("*****************************************");
                    System.out.println("*\t FROM: " + sender + "\t\t\t*");
                    System.out.println("*\t SENT: " + timestamp + "\t*");
                } while (rs.next());

                System.out.println("*****************************************");
                
            }

            if (hasRequests) {
                System.out.println("\n1 - Accept/Reject a friend request");
                System.out.println("2 - Back to previous page");
                System.out.print("What would you like to do?: ");
                String choice = input.nextLine();

                if(choice.equals("1")) acceptOrRejectRequest(username, stmt, input);
                else manageAccountHome(username, stmt, input);
            }
            else {
                System.out.print("Press Enter to return to previous page.");
                input.nextLine();
                manageAccountHome(username, stmt, input);
            }
            rs.close();
         }
        catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
        }

    }

    // Shows a list of all outgoing friend requests, and offers the option to cancel them.
    private static void viewOutgoingRequests(String username, Statement stmt, Scanner input) {
        String query = String.format("SELECT receiver, timestamp FROM friendrequests WHERE sender=\'%s\' AND status='PENDING';", username);
        ResultSet rs = Main.SQL_select(query, stmt);
        boolean hasRequests = false;

        System.out.println("\n         OUTGOING FRIEND REQUESTS      ");
        System.out.println("=========================================");

        try {
            if(!rs.next()) {
                System.out.println("\n*****************************************");
                System.out.println("*      No outgoing requests found       *");
                System.out.println("*****************************************");
            }
            else {
                hasRequests = true;

                do {
                    String receiver = rs.getString("receiver");
                    String timestamp = rs.getString("timestamp");
                    
                    System.out.println("*****************************************");
                    System.out.println("*\t TO: " + receiver + "\t\t\t*");
                    System.out.println("*\t SENT: " + timestamp + "\t*");
                } while (rs.next());

                System.out.println("*****************************************");
                
            }

            if (hasRequests) {
                System.out.println("\n1 - Cancel a friend request");
                System.out.println("2 - Back to previous page");
                System.out.print("What would you like to do?: ");
                String choice = input.nextLine();

                if(choice.equals("1")) cancelRequest(username, stmt, input);
                else manageAccountHome(username, stmt, input);
            }
            else {
                System.out.print("Press Enter to return to previous page.");
                input.nextLine();
                manageAccountHome(username, stmt, input);
            }
            rs.close();
         }
        catch (Exception e) {
			e.printStackTrace();
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(1);
        }
    }

    // Makes a friend request from user SENDER to user RECEIVER by inserting a new row into the friendrequests table.
    private static void makeFriendRequest(String sender, String receiver, Statement stmt) {
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());

        String sqlInsert = String.format("INSERT INTO friendrequests (sender, receiver, timestamp, status, responsetimestamp)" +
                                  "VALUES (\'%s\', \'%s\', \'%s\', \'PENDING\', null);", sender, receiver, timestamp, timestamp);
        Main.SQL_insert(sqlInsert, stmt);
        System.out.println("Friend request to " +  receiver + " has successfully been made!");
    }

    // This function will see if a friend request can be made by the current user to the user they specify. If a friend request
    // cannot be made for some reason, the function will display the reason. If a request can be made, then the makeFriendRequest()
    // function will be called.
    private static void attemptFriendRequest(String username, Statement stmt, Scanner input) {
        System.out.print("Please enter the username of the user you would like to send a Friend Request to (Just press Enter to go back): ");
        String requestUsername = input.nextLine();

        // Go back to Manage Account Home
        if(requestUsername.equals(""))
            manageAccountHome(username, stmt, input);
        else if (requestUsername.equals(username)) {
            System.out.println("You cannot send a friend request to yourself.");
            manageAccountHome(username, stmt, input);
        }
        else {
            try {
                String query = String.format("SELECT * FROM friendrequests " +
                                            "WHERE (sender = \'%s\' AND receiver = \'%s\') OR (receiver = \'%s\' AND sender = \'%s\');", username, requestUsername, username, requestUsername);
                ResultSet rs = Main.SQL_select(query, stmt);

                if(rs.next()) {
                    String sender = rs.getString("sender");
                    String receiver = rs.getString("receiver");
                    String status = rs.getString("status");
                    
                    if(sender.equals(username) && status.equals("PENDING")) {
                        System.out.println("You already have a pending friend request to this user. Please wait for their response.");
                    }
                    else if(receiver.equals(username) && status.equals("PENDING")) {
                        System.out.println("This user has already sent a friend request to you. Go to your incoming friend requests to accept.");
                    }
                    else if(sender.equals(username) && status.equals("REJECTED")) {
                        String timestamp = rs.getString("responsetimestamp");

                        Date d1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(timestamp);
                        Date d2 = new Date();

                        // The time between now and when the request was responded to in minutes
                        long timeDifference = ((d2.getTime() - d1.getTime()) / (1000 * 60)) % 60;

                        if(timeDifference > 5) {
                            // Delete old friend request
                            String sqlUpdate = String.format("DELETE FROM friendrequests WHERE sender = \'%s\' AND receiver = \'%s\';", username, requestUsername);
                            Main.SQL_insert(sqlUpdate, stmt);
                            makeFriendRequest(username, requestUsername, stmt);
                        } 
                        else {
                            System.out.println("It has been less than 5 minutes since your last request has been rejected, or since you were deleted as a friend.\nYou must wait before you can send a friend request to this user.");
                        }
                    }
                }
                else {

                    //Make sure user that they want to send a friend request to exists
                    if(userExists(requestUsername, stmt))
                        //Send Friend Request
                        makeFriendRequest(username, requestUsername, stmt);
                    else
                        System.out.println("Could not find any user with username "  + requestUsername);
                    
                }
                rs.close();
                manageAccountHome(username, stmt, input);
            }
            catch (Exception e) {
                e.printStackTrace();
                System.err.println(e.getClass().getName() + ": " + e.getMessage());
                System.exit(1);
            }
        }
    }

    public static void manageAccountHome(String username, Statement stmt, Scanner input) {        

        System.out.println("\n1 - View Friends List");
        System.out.println("2 - View Incoming Friend Requests");
        System.out.println("3 - View Outgoing Friend Requests");
        System.out.println("4 - Make a new Friend Request");
        System.out.println("5 - Back to previous page");
        System.out.print("What would you like to do: ");
        String choice = input.nextLine();

        switch(choice) {
            case "1":
                viewFriendsList(username, stmt, input);
                break;
            case "2":
                viewIncomingRequests(username, stmt, input);
                break;
            case "3":
                viewOutgoingRequests(username, stmt, input);
                break;
            case "4":
                attemptFriendRequest(username, stmt, input);
                break;
            case "5":
                Main.navigationPage(username, stmt, input);
        }

    }
    
}
