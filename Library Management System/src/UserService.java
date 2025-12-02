import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class UserService {

    Scanner sc = new Scanner(System.in);
    int userId;

    public UserService(int userId) {
        this.userId = userId;
    }

    public void borrowBook() {
        try {
            System.out.print("Enter Book ID to Borrow: ");
            int bookId = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();

            // Check quantity
            PreparedStatement ps1 = con.prepareStatement("SELECT quantity FROM books WHERE bid=?");
            ps1.setInt(1, bookId);

            ResultSet rs = ps1.executeQuery();

            if (!rs.next()) {
                System.out.println("Invalid Book ID!");
                return;
            }

            int qty = rs.getInt("quantity");
            if (qty <= 0) {
                System.out.println("Book Not Available!");
                return;
            }

            // Reduce quantity
            PreparedStatement ps2 = con.prepareStatement("UPDATE books SET quantity=quantity-1 WHERE bid=?");
            ps2.setInt(1, bookId);
            ps2.executeUpdate();

            // Add to borrow table
            PreparedStatement ps3 = con.prepareStatement("INSERT INTO borrow(user_id, book_id, borrow_date) VALUES(?,?,?)");

            ps3.setInt(1, userId);
            ps3.setInt(2, bookId);
            ps3.setDate(3, java.sql.Date.valueOf(LocalDate.now()));

            ps3.executeUpdate();
            System.out.println("Book Borrowed Successfully!");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void returnBook() {
        try {
            System.out.print("Enter Book ID to Return: ");
            int bookId = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();

            // Update quantity
            PreparedStatement ps1 = con.prepareStatement("UPDATE books SET quantity = quantity + 1 WHERE bid=?");

            ps1.setInt(1, bookId);
            ps1.executeUpdate();

            // Update return date
            PreparedStatement ps2 = con.prepareStatement(
                    "UPDATE borrow SET return_date=? WHERE user_id=? AND book_id=? AND return_date IS NULL");

            ps2.setDate(1, java.sql.Date.valueOf(LocalDate.now()));
            ps2.setInt(2, userId);
            ps2.setInt(3, bookId);

            int rows = ps2.executeUpdate();

            if (rows > 0)
                System.out.println("Book Returned Successfully!");
            else
                System.out.println("You have not borrowed this book!");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void viewAvailableBooks() {
        try {
            Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM books");

            System.out.println("--- Available Books ---");
            while (rs.next()) {
                System.out.println("Book ID: "+rs.getInt("bid"));
                System.out.println("Book Title: "+rs.getString("title"));
                System.out.println("Author Name: "+rs.getString("author"));
                System.out.println("Quantity: "+rs.getInt("quantity"));
                System.out.println("......");
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void searchBookByTitle() {
        try {
            System.out.print("Enter Book Title: ");
            String title = sc.nextLine();

            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM books WHERE title LIKE ?");

            ps.setString(1, "%" + title + "%");

            ResultSet rs = ps.executeQuery();

            System.out.println("--- Search Results ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("Book ID: "+rs.getInt("bid"));
                System.out.println("Book Title: "+rs.getString("title"));
                System.out.println("Author Name: "+rs.getString("author"));
                System.out.println("Quantity: "+rs.getInt("quantity"));
                System.out.println("......");
            }

            if (!found)
                System.out.println("No books found with that title!");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void searchBookByAuthor() {
        try {
            System.out.print("Enter Author Name: ");
            String author = sc.nextLine();

            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM books WHERE author LIKE ?");

            ps.setString(1, "%" + author + "%");

            ResultSet rs = ps.executeQuery();

            System.out.println("--- Search Results ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("Book ID: "+rs.getInt("bid"));
                System.out.println("Book Title: "+rs.getString("title"));
                System.out.println("Author Name: "+rs.getString("author"));
                System.out.println("Quantity: "+rs.getInt("quantity"));
                System.out.println("......");
            }

            if (!found)
                System.out.println("No books found for that author!");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}