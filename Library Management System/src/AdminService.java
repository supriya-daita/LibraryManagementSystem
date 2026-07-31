import java.sql.*;
import java.util.*;

/**
 * AdminService.java
 * -----------------
 * Provides all administrative operations for the Library Management System.
 * An admin can manage the book catalogue and monitor borrow activity.
 *
 * Operations available:
 *  - addBook()              : Insert a new book record into the books table.
 *  - deleteBook()           : Remove a book by its ID from the books table.
 *  - viewBooks()            : Display all book records.
 *  - updateBookQuantity()   : Change the quantity of a specific book.
 *  - searchBookById()       : Find a single book by its unique ID.
 *  - searchBookByTitle()    : Find books whose title matches a keyword.
 *  - searchBookByAuthor()   : Find books whose author matches a keyword.
 *  - viewZeroQuantityBooks(): List books that are currently out of stock.
 *  - viewBorrowedBooks()    : List all borrow records with user/book details.
 *
 * Each method obtains its own DB connection via DBConnection.getConnection().
 * All SQL exceptions are caught, logged with context, and gracefully handled
 * so the application does not crash on a DB error.
 */
public class AdminService {

    // Scanner is shared across all admin operations within the same session.
    Scanner sc = new Scanner(System.in);

    // -----------------------------------------------------------------------
    // ADD BOOK
    // -----------------------------------------------------------------------

    /**
     * Reads book details from the console and inserts a new record into the
     * {@code books} table.
     *
     * Fields collected:
     *  - bid      : unique integer Book ID
     *  - title    : book title (must be unique per the DB UNIQUE constraint)
     *  - author   : author name
     *  - quantity : number of copies available
     *
     * Possible errors handled:
     *  - Duplicate bid or title → SQLIntegrityConstraintViolationException
     *  - DB connection failure  → NullPointerException / SQLException
     */
    public void addBook() {
        System.out.print("Enter Book ID: ");
        int id = -1;

        // Validate numeric input for Book ID
        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID. Please enter a numeric value.");
            return;
        }

        System.out.print("Enter Book Title: ");
        String title = sc.nextLine().trim();

        System.out.print("Enter Author Name: ");
        String author = sc.nextLine().trim();

        System.out.print("Enter Quantity: ");
        int qty = -1;

        // Validate numeric input for Quantity
        try {
            qty = Integer.parseInt(sc.nextLine().trim());
            if (qty < 0) {
                System.out.println("[Error] Quantity cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid quantity. Please enter a numeric value.");
            return;
        }

        // Attempt to insert the book into the database
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database. Please try again later.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO books(bid, title, author, quantity) VALUES(?,?,?,?)")) {

            ps.setInt(1, id);
            ps.setString(2, title);
            ps.setString(3, author);
            ps.setInt(4, qty);

            ps.executeUpdate();
            System.out.println("Book Added Successfully!");

        } catch (SQLIntegrityConstraintViolationException e) {
            // Triggered when bid or title already exists (UNIQUE constraint)
            System.out.println("[Error] A book with this ID or title already exists.");
            System.out.println("  Detail: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("[Error] Failed to add book.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // DELETE BOOK
    // -----------------------------------------------------------------------

    /**
     * Reads a Book ID from the console and deletes the corresponding record
     * from the {@code books} table.
     *
     * Prints "Book Deleted!" if a row was affected, or "Invalid Book ID"
     * if no matching record was found.
     *
     * Possible errors handled:
     *  - Non-numeric input  → NumberFormatException
     *  - DB failure         → SQLException
     */
    public void deleteBook() {
        System.out.print("Enter Book ID to Delete: ");
        int id;

        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID. Please enter a numeric value.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM books WHERE bid=?")) {

            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            // executeUpdate() returns the number of rows affected
            if (rows > 0)
                System.out.println("Book Deleted!");
            else
                System.out.println("Invalid Book ID. No book found with ID: " + id);

        } catch (SQLIntegrityConstraintViolationException e) {
            // May occur if the book is referenced by the borrow table (FK constraint)
            System.out.println("[Error] Cannot delete this book — it is referenced in borrow records.");
            System.out.println("  Detail: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("[Error] Failed to delete book.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // VIEW ALL BOOKS
    // -----------------------------------------------------------------------

    /**
     * Fetches and prints all records from the {@code books} table.
     * Displays Book ID, Title, Author, and Quantity for each entry.
     */
    public void viewBooks() {
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database.");
            return;
        }

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM books")) {

            System.out.println("--- All Books ---");
            boolean hasRows = false;

            while (rs.next()) {
                hasRows = true;
                System.out.println("Book ID: "     + rs.getInt("bid"));
                System.out.println("Book Title: "  + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("Quantity: "    + rs.getInt("quantity"));
                System.out.println("......");
            }

            if (!hasRows) {
                System.out.println("No books found in the library.");
            }

        } catch (SQLException e) {
            System.out.println("[Error] Failed to retrieve books.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // UPDATE BOOK QUANTITY
    // -----------------------------------------------------------------------

    /**
     * Reads a Book ID and a new quantity from the console, then updates the
     * {@code quantity} column in the {@code books} table.
     *
     * Possible errors handled:
     *  - Non-numeric input for ID or quantity → NumberFormatException
     *  - Negative quantity → manual validation
     *  - DB failure → SQLException
     */
    public void updateBookQuantity() {
        System.out.print("Enter Book ID to Update Quantity: ");
        int id;

        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID. Please enter a numeric value.");
            return;
        }

        System.out.print("Enter New Quantity: ");
        int qty;

        try {
            qty = Integer.parseInt(sc.nextLine().trim());
            if (qty < 0) {
                System.out.println("[Error] Quantity cannot be negative.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid quantity. Please enter a numeric value.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE books SET quantity=? WHERE bid=?")) {

            ps.setInt(1, qty);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Book Quantity Updated Successfully!");
            else
                System.out.println("Invalid Book ID. No book found with ID: " + id);

        } catch (SQLException e) {
            System.out.println("[Error] Failed to update book quantity.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // SEARCH BY ID
    // -----------------------------------------------------------------------

    /**
     * Searches for a book by its exact Book ID and prints its details.
     * Uses a parameterized query to prevent SQL injection.
     */
    public void searchBookById() {
        System.out.print("Enter Book ID: ");
        int id;

        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID. Please enter a numeric value.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM books WHERE bid=?")) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("--- Book Found ---");
                    System.out.println("Book ID: "     + rs.getInt("bid"));
                    System.out.println("Book Title: "  + rs.getString("title"));
                    System.out.println("Author Name: " + rs.getString("author"));
                    System.out.println("Quantity: "    + rs.getInt("quantity"));
                } else {
                    System.out.println("No book found with ID: " + id);
                }
            }

        } catch (SQLException e) {
            System.out.println("[Error] Failed to search for book.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // SEARCH BY TITLE
    // -----------------------------------------------------------------------

    /**
     * Searches for books whose title contains the given keyword (case-insensitive
     * LIKE search). Prints all matching results.
     */
    public void searchBookByTitle() {
        System.out.print("Enter Book Title: ");
        String title = sc.nextLine().trim();

        if (title.isEmpty()) {
            System.out.println("[Error] Title cannot be empty.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM books WHERE title LIKE ?")) {

            // Wrap the keyword with % wildcards for a partial match
            ps.setString(1, "%" + title + "%");

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("--- Search Results ---");
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println("Book ID: "     + rs.getInt("bid"));
                    System.out.println("Book Title: "  + rs.getString("title"));
                    System.out.println("Author Name: " + rs.getString("author"));
                    System.out.println("Quantity: "    + rs.getInt("quantity"));
                    System.out.println("......");
                }

                if (!found)
                    System.out.println("No books found with title containing: \"" + title + "\"");
            }

        } catch (SQLException e) {
            System.out.println("[Error] Failed to search books by title.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // SEARCH BY AUTHOR
    // -----------------------------------------------------------------------

    /**
     * Searches for books whose author name contains the given keyword
     * (case-insensitive LIKE search). Prints all matching results.
     */
    public void searchBookByAuthor() {
        System.out.print("Enter Author Name: ");
        String author = sc.nextLine().trim();

        if (author.isEmpty()) {
            System.out.println("[Error] Author name cannot be empty.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM books WHERE author LIKE ?")) {

            ps.setString(1, "%" + author + "%");

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("--- Search Results ---");
                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println("Book ID: "     + rs.getInt("bid"));
                    System.out.println("Book Title: "  + rs.getString("title"));
                    System.out.println("Author Name: " + rs.getString("author"));
                    System.out.println("Quantity: "    + rs.getInt("quantity"));
                    System.out.println("......");
                }

                if (!found)
                    System.out.println("No books found for author containing: \"" + author + "\"");
            }

        } catch (SQLException e) {
            System.out.println("[Error] Failed to search books by author.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // VIEW ZERO-QUANTITY BOOKS
    // -----------------------------------------------------------------------

    /**
     * Retrieves and displays all books where quantity = 0 (out of stock).
     * These books cannot be borrowed by users until restocked.
     */
    public void viewZeroQuantityBooks() {
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database.");
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM books WHERE quantity = 0");
             ResultSet rs = ps.executeQuery()) {

            System.out.println("--- Out of Stock Books ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("Book ID: "     + rs.getInt("bid"));
                System.out.println("Book Title: "  + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("Quantity: "    + rs.getInt("quantity"));
                System.out.println("......");
            }

            if (!found)
                System.out.println("All books are currently in stock!");

        } catch (SQLException e) {
            System.out.println("[Error] Failed to retrieve out-of-stock books.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // VIEW BORROWED BOOKS
    // -----------------------------------------------------------------------

    /**
     * Fetches all records from the {@code borrow} table joined with the
     * {@code users} and {@code books} tables to provide full context.
     *
     * Displays:
     *  - Borrow ID, Username, Book Title, Author, Borrow Date, Return Date.
     *  - Return Date is shown as "Book Not Returned!" when still null.
     */
    public void viewBorrowedBooks() {
        Connection con = DBConnection.getConnection();
        if (con == null) {
            System.out.println("[Error] Unable to connect to the database.");
            return;
        }

        // JOIN query to get human-readable user and book info alongside borrow records
        String query =
            "SELECT b.borrow_id, u.username, bk.title, bk.author, b.borrow_date, b.return_date " +
            "FROM borrow b " +
            "JOIN users u  ON b.user_id  = u.user_id " +
            "JOIN books bk ON b.book_id  = bk.bid";

        try (PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("--- Borrowed Books List ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("Borrow ID: "  + rs.getInt("borrow_id"));
                System.out.println("User Name: "  + rs.getString("username"));
                System.out.println("Book Title: " + rs.getString("title"));
                System.out.println("Author: "     + rs.getString("author"));
                System.out.println("Borrow Date: "+ rs.getDate("borrow_date"));

                // Return date is nullable — display a message when NULL
                Date returnDate = rs.getDate("return_date");
                if (returnDate == null)
                    System.out.println("Return Status: Book Not Returned!");
                else
                    System.out.println("Return Date: " + returnDate);

                System.out.println("......");
            }

            if (!found)
                System.out.println("No active borrow records found!");

        } catch (SQLException e) {
            System.out.println("[Error] Failed to retrieve borrow records.");
            System.out.println("  SQL State: " + e.getSQLState() + " | " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    // -----------------------------------------------------------------------
    // HELPER: CLOSE CONNECTION
    // -----------------------------------------------------------------------

    /**
     * Safely closes a database connection if it is not null.
     * Called in every method's finally block to avoid connection leaks.
     *
     * @param con the Connection to close (may be null)
     */
    private void closeConnection(Connection con) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                System.err.println("[Warning] Failed to close DB connection: " + e.getMessage());
            }
        }
    }
}