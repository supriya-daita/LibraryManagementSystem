import java.util.*;
import java.sql.*;

/**
 * LibrarySystem.java
 * ------------------
 * Entry point for the Command Line Interface (CLI) Library Management System.
 * 
 * Features:
 *  - User Authentication (Admin / User roles)
 *  - Dynamic menu navigation based on authenticated role
 *  - Exception handling for invalid inputs and database execution errors
 */
public class LibrarySystem {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("---------- Library Management System ----------");

        System.out.print("Username: ");
        String username = sc.nextLine().trim();

        System.out.print("Password: ");
        String password = sc.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("[Error] Username and Password cannot be blank!");
            return;
        }

        // Establish connection to database for authentication
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Could not connect to the database. Exiting application.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE username=? AND password=?")) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    int userId = rs.getInt("user_id");

                    System.out.println("\n[Success] Logged in as " + username + " (" + role.toUpperCase() + ")");

                    // Delegate execution to menu handlers based on user role
                    if ("admin".equalsIgnoreCase(role)) {
                        adminMenu();
                    } else {
                        userMenu(userId);
                    }
                } else {
                    System.out.println("[Error] Invalid Login Credentials!");
                }
            }
        } catch (SQLException e) {
            System.out.println("[Error] Database query failure during authentication.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            try {
                if (con != null) con.close();
            } catch (SQLException e) {
                System.err.println("[Warning] Failed to close DB connection: " + e.getMessage());
            }
        }
    }

    /**
     * Admin menu control loop. Prompts admin for choices and executes AdminService methods.
     */
    public static void adminMenu() {
        Scanner sc = new Scanner(System.in);
        AdminService admin = new AdminService();

        while (true) {
            System.out.println("\n------ Admin Menu ------");
            System.out.println("1. Add Book");
            System.out.println("2. Delete Book");
            System.out.println("3. View Books");
            System.out.println("4. Update Book Quantity");
            System.out.println("5. Search Book by ID");
            System.out.println("6. Search Book by Title");
            System.out.println("7. Search Book by Author");
            System.out.println("8. View Zero Quantity Books");
            System.out.println("9. View Borrowed Books");
            System.out.println("10. Logout");
            System.out.print("Choose option (1-10): ");

            int choice = -1;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid menu option. Please enter a number between 1 and 10.");
                continue;
            }

            switch (choice) {
                case 1  -> admin.addBook();
                case 2  -> admin.deleteBook();
                case 3  -> admin.viewBooks();
                case 4  -> admin.updateBookQuantity();
                case 5  -> admin.searchBookById();
                case 6  -> admin.searchBookByTitle();
                case 7  -> admin.searchBookByAuthor();
                case 8  -> admin.viewZeroQuantityBooks();
                case 9  -> admin.viewBorrowedBooks();
                case 10 -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("[Error] Invalid choice! Please select between 1 and 10.");
            }
        }
    }

    /**
     * User menu control loop. Prompts user for choices and executes UserService methods.
     * 
     * @param userId The ID of the authenticated user.
     */
    public static void userMenu(int userId) {
        Scanner sc = new Scanner(System.in);
        UserService user = new UserService(userId);

        while (true) {
            System.out.println("\n------ User Menu ------");
            System.out.println("1. View Available Books");
            System.out.println("2. Borrow Book");
            System.out.println("3. Return Book");
            System.out.println("4. Search Book by Title");
            System.out.println("5. Search Book by Author");
            System.out.println("6. Logout");
            System.out.print("Choose option (1-6): ");

            int choice = -1;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid menu option. Please enter a number between 1 and 6.");
                continue;
            }

            switch (choice) {
                case 1  -> user.viewAvailableBooks();
                case 2  -> user.borrowBook();
                case 3  -> user.returnBook();
                case 4  -> user.searchBookByTitle();
                case 5  -> user.searchBookByAuthor();
                case 6  -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("[Error] Invalid choice! Please select between 1 and 6.");
            }
        }
    }
}