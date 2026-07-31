import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * UserService.java
 * ----------------
 * Provides user-level operations for the Library Management System.
 * Regular users (non-admins) can view, search, borrow, and return books.
 *
 * Operations available:
 *  - borrowBook()        : Decrements stock quantity and creates a record in `borrow`.
 *  - returnBook()        : Increments stock quantity and updates return_date in `borrow`.
 *  - viewAvailableBooks(): Displays all books in the library catalogue.
 *  - searchBookByTitle() : Searches books matching a given title pattern.
 *  - searchBookByAuthor(): Searches books matching a given author pattern.
 *
 * Each instance of UserService is tied to a specific logged-in `userId`.
 */
public class UserService {

    private Scanner sc = new Scanner(System.in);
    private int userId;

    /**
     * Constructs a UserService bound to the given user ID.
     * 
     * @param userId Unique ID of the currently logged-in user.
     */
    public UserService(int userId) {
        this.userId = userId;
    }

    /**
     * Handles borrowing a book for the current user.
     * 
     * Process:
     *  1. Prompts for Book ID.
     *  2. Checks if book exists and has quantity > 0.
     *  3. Decrements book quantity in `books` table.
     *  4. Inserts borrow record into `borrow` table with today's date.
     * 
     * Handles input validation and SQL exceptions gracefully.
     */
    public void borrowBook() {
        System.out.print("Enter Book ID to Borrow: ");
        int bookId = -1;

        try {
            bookId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID. Please enter a numeric value.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Database connection failed. Please try again.");
            return;
        }

        try {
            // Check quantity availability
            PreparedStatement ps1 = con.prepareStatement("SELECT quantity FROM books WHERE bid=?");
            ps1.setInt(1, bookId);
            ResultSet rs = ps1.executeQuery();

            if (!rs.next()) {
                System.out.println("[Error] Invalid Book ID! Book does not exist.");
                return;
            }

            int qty = rs.getInt("quantity");
            if (qty <= 0) {
                System.out.println("[Notice] Book is currently out of stock!");
                return;
            }

            // Reduce quantity in books table
            PreparedStatement ps2 = con.prepareStatement("UPDATE books SET quantity = quantity - 1 WHERE bid=?");
            ps2.setInt(1, bookId);
            ps2.executeUpdate();

            // Add record into borrow table
            PreparedStatement ps3 = con.prepareStatement("INSERT INTO borrow(user_id, book_id, borrow_date) VALUES(?,?,?)");
            ps3.setInt(1, userId);
            ps3.setInt(2, bookId);
            ps3.setDate(3, java.sql.Date.valueOf(LocalDate.now()));
            ps3.executeUpdate();

            System.out.println("Book Borrowed Successfully!");

        } catch (SQLException e) {
            System.out.println("[Error] Database operation failed during borrow process.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    /**
     * Handles returning a borrowed book for the current user.
     * 
     * Process:
     *  1. Prompts for Book ID to return.
     *  2. Updates `borrow` table to set `return_date` to today where return_date IS NULL.
     *  3. Increments book quantity in `books` table if return record updated.
     */
    public void returnBook() {
        System.out.print("Enter Book ID to Return: ");
        int bookId = -1;

        try {
            bookId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID. Please enter a numeric value.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Database connection failed. Please try again.");
            return;
        }

        try {
            // Update return_date for active borrow record
            PreparedStatement ps2 = con.prepareStatement(
                    "UPDATE borrow SET return_date=? WHERE user_id=? AND book_id=? AND return_date IS NULL");

            ps2.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            ps2.setInt(2, userId);
            ps2.setInt(3, bookId);

            int rows = ps2.executeUpdate();

            if (rows > 0) {
                // Increment book stock quantity back
                PreparedStatement ps1 = con.prepareStatement("UPDATE books SET quantity = quantity + 1 WHERE bid=?");
                ps1.setInt(1, bookId);
                ps1.executeUpdate();

                System.out.println("Book Returned Successfully!");
            } else {
                System.out.println("[Notice] You do not have an active borrow record for this Book ID.");
            }

        } catch (SQLException e) {
            System.out.println("[Error] Database operation failed during return process.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    /**
     * Views all books available in the library database.
     */
    public void viewAvailableBooks() {
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Database connection failed. Please try again.");
            return;
        }

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM books")) {

            System.out.println("--- Available Books ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("Book ID    : " + rs.getInt("bid"));
                System.out.println("Book Title : " + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("Quantity   : " + rs.getInt("quantity"));
                System.out.println("......");
            }

            if (!found) {
                System.out.println("No books are currently present in the catalogue.");
            }

        } catch (SQLException e) {
            System.out.println("[Error] Failed to fetch available books.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    /**
     * Searches books by title using SQL LIKE keyword matching.
     */
    public void searchBookByTitle() {
        System.out.print("Enter Book Title: ");
        String title = sc.nextLine().trim();

        if (title.isEmpty()) {
            System.out.println("[Error] Title query cannot be empty.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Database connection failed.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM books WHERE title LIKE ?")) {
            ps.setString(1, "%" + title + "%");
            ResultSet rs = ps.executeQuery();

            System.out.println("--- Search Results ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("Book ID    : " + rs.getInt("bid"));
                System.out.println("Book Title : " + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("Quantity   : " + rs.getInt("quantity"));
                System.out.println("......");
            }

            if (!found) {
                System.out.println("No books found matching title: " + title);
            }

        } catch (SQLException e) {
            System.out.println("[Error] Failed to search books by title.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    /**
     * Searches books by author using SQL LIKE keyword matching.
     */
    public void searchBookByAuthor() {
        System.out.print("Enter Author Name: ");
        String author = sc.nextLine().trim();

        if (author.isEmpty()) {
            System.out.println("[Error] Author query cannot be empty.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Database connection failed.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM books WHERE author LIKE ?")) {
            ps.setString(1, "%" + author + "%");
            ResultSet rs = ps.executeQuery();

            System.out.println("--- Search Results ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("Book ID    : " + rs.getInt("bid"));
                System.out.println("Book Title : " + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("Quantity   : " + rs.getInt("quantity"));
                System.out.println("......");
            }

            if (!found) {
                System.out.println("No books found for author: " + author);
            }

        } catch (SQLException e) {
            System.out.println("[Error] Failed to search books by author.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    /**
     * Closes SQL connection safely.
     */
    private void closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                System.err.println("[Warning] Failed to close connection: " + e.getMessage());
            }
        }
    }
}