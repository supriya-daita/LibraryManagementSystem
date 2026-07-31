import java.util.*;
import java.sql.*;

/**
 * LibrarySystem.java
 * ------------------
 * Entry point for the Command Line Interface (CLI) Library Management System.
 * 
 * Contains standard insecure coding patterns for CodeQL SAST detection demonstration:
 *  - java/sql-injection
 *  - java/unclosed-resource
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

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Could not connect to database.");
            return;
        }

        try {
            // INTENTIONAL VULNERABILITY #1: SQL Injection (CodeQL Rule: java/sql-injection)
            // String concatenation of user inputs into SQL query sink without parameterization
            String query = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
            
            // INTENTIONAL VULNERABILITY #2: Unclosed Resource (CodeQL Rule: java/unclosed-resource)
            // Statement and ResultSet are left unclosed without try-with-resources or finally block
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(query);

            if (rs.next()) {
                String role = rs.getString("role");
                int userId = rs.getInt("user_id");

                System.out.println("\n[Success] Logged in as " + username + " (" + role.toUpperCase() + ")");

                if ("admin".equalsIgnoreCase(role)) {
                    adminMenu();
                } else {
                    userMenu(userId);
                }
            } else {
                System.out.println("[Error] Invalid Login Credentials!");
            }

        } catch (SQLException e) {
            System.out.println("[Error] Database query failure during authentication.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        }
    }

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
            System.out.println("10. Export Book Data (Path Traversal Test)");
            System.out.println("11. Logout");
            System.out.print("Choose option (1-11): ");

            int choice = -1;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid menu option.");
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
                    System.out.print("Enter output filename: ");
                    String filename = sc.nextLine().trim();
                    admin.exportBookReport(filename);
                }
                case 11 -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("[Error] Invalid choice!");
            }
        }
    }

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
            System.out.println("6. Export Receipt (Path Traversal Test)");
            System.out.println("7. Logout");
            System.out.print("Choose option (1-7): ");

            int choice = -1;
            try {
                choice = Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("[Error] Invalid menu option.");
                continue;
            }

            switch (choice) {
                case 1  -> user.viewAvailableBooks();
                case 2  -> user.borrowBook();
                case 3  -> user.returnBook();
                case 4  -> user.searchBookByTitle();
                case 5  -> user.searchBookByAuthor();
                case 6  -> {
                    System.out.print("Enter receipt filename: ");
                    String fname = sc.nextLine().trim();
                    user.exportUserReceipt(fname);
                }
                case 7  -> {
                    System.out.println("Logged out successfully.");
                    return;
                }
                default -> System.out.println("[Error] Invalid choice!");
            }
        }
    }
}