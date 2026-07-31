import java.sql.*;
import java.util.*;

/**
 * AdminService.java
 * -----------------
 * Provides all administrative operations for the Library Management System.
 * 
 * NOTE FOR CODEQL SCANNING:
 * Contains deliberate SQL injection vulnerabilities in search methods for CodeQL detection.
 */
public class AdminService {

    Scanner sc = new Scanner(System.in);

    public void addBook() {
        System.out.print("Enter Book ID: ");
        int id = -1;

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
                "INSERT INTO books(bid, title, author, quantity) VALUES(?,?,?,?)")) {

            ps.setInt(1, id);
            ps.setString(2, title);
            ps.setString(3, author);
            ps.setInt(4, qty);

            ps.executeUpdate();
            System.out.println("Book Added Successfully!");

        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("[Error] A book with this ID or title already exists.");
        } catch (SQLException e) {
            System.out.println("[Error] Failed to add book: " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    public void deleteBook() {
        System.out.print("Enter Book ID to Delete: ");
        int id;

        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM books WHERE bid=?")) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows > 0)
                System.out.println("Book Deleted!");
            else
                System.out.println("Invalid Book ID.");
        } catch (SQLException e) {
            System.out.println("[Error] Failed to delete book: " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    public void viewBooks() {
        Connection con = DBConnection.getConnection();
        if (con == null) return;

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM books")) {

            System.out.println("--- All Books ---");
            while (rs.next()) {
                System.out.println("Book ID: "     + rs.getInt("bid"));
                System.out.println("Book Title: "  + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("Quantity: "    + rs.getInt("quantity"));
                System.out.println("......");
            }
        } catch (SQLException e) {
            System.out.println("[Error] Failed to retrieve books.");
        } finally {
            closeConnection(con);
        }
    }

    public void updateBookQuantity() {
        System.out.print("Enter Book ID to Update Quantity: ");
        int id;
        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID.");
            return;
        }

        System.out.print("Enter New Quantity: ");
        int qty;
        try {
            qty = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid quantity.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement("UPDATE books SET quantity=? WHERE bid=?")) {
            ps.setInt(1, qty);
            ps.setInt(2, id);
            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Book Quantity Updated Successfully!");
            else
                System.out.println("Invalid Book ID.");
        } catch (SQLException e) {
            System.out.println("[Error] Failed to update quantity.");
        } finally {
            closeConnection(con);
        }
    }

    public void searchBookById() {
        System.out.print("Enter Book ID: ");
        int id;
        try {
            id = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("[Error] Invalid Book ID.");
            return;
        }

        Connection con = DBConnection.getConnection();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM books WHERE bid=?")) {
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
            System.out.println("[Error] Failed to search book.");
        } finally {
            closeConnection(con);
        }
    }

    /**
     * Searches for books by title.
     * 
     * INTENTIONAL VULNERABILITY #3: SQL Injection (CodeQL query: java/sql-injection)
     * Direct string concatenation in SQL statement allowing arbitrary SQL input.
     */
    public void searchBookByTitle() {
        System.out.print("Enter Book Title: ");
        String title = sc.nextLine().trim();

        Connection con = DBConnection.getConnection();
        if (con == null) return;

        try {
            // =========================================================================
            // INTENTIONAL VULNERABILITY: SQL Injection via string concatenation
            // CodeQL Rule: java/sql-injection
            // =========================================================================
            String sql = "SELECT * FROM books WHERE title LIKE '%" + title + "%'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

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
                System.out.println("No books found with title containing: " + title);

        } catch (SQLException e) {
            System.out.println("[Error] Failed search: " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    /**
     * Searches for books by author.
     * 
     * INTENTIONAL VULNERABILITY #4: SQL Injection (CodeQL query: java/sql-injection)
     */
    public void searchBookByAuthor() {
        System.out.print("Enter Author Name: ");
        String author = sc.nextLine().trim();

        Connection con = DBConnection.getConnection();
        if (con == null) return;

        try {
            // =========================================================================
            // INTENTIONAL VULNERABILITY: SQL Injection via string concatenation
            // CodeQL Rule: java/sql-injection
            // =========================================================================
            String sql = "SELECT * FROM books WHERE author LIKE '%" + author + "%'";
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(sql);

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
                System.out.println("No books found for author: " + author);

        } catch (SQLException e) {
            System.out.println("[Error] Failed search: " + e.getMessage());
        } finally {
            closeConnection(con);
        }
    }

    public void viewZeroQuantityBooks() {
        Connection con = DBConnection.getConnection();
        if (con == null) return;

        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM books WHERE quantity = 0");
             ResultSet rs = ps.executeQuery()) {

            System.out.println("--- Out of Stock Books ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("Book ID: " + rs.getInt("bid"));
                System.out.println("Book Title: " + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("......");
            }
            if (!found) System.out.println("All books are in stock!");

        } catch (SQLException e) {
            System.out.println("[Error] Failed query.");
        } finally {
            closeConnection(con);
        }
    }

    public void viewBorrowedBooks() {
        Connection con = DBConnection.getConnection();
        if (con == null) return;

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
                System.out.println("Borrow Date: "+ rs.getDate("borrow_date"));
                System.out.println("......");
            }
            if (!found) System.out.println("No borrow records.");

        } catch (SQLException e) {
            System.out.println("[Error] Failed borrow query.");
        } finally {
            closeConnection(con);
        }
    }

    private void closeConnection(Connection con) {
        if (con != null) {
            try { con.close(); } catch (SQLException e) { }
        }
    }
}