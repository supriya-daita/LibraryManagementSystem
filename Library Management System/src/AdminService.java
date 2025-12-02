import java.sql.*;
import java.util.*;

public class AdminService {

    Scanner sc = new Scanner(System.in);

    public void addBook() {
        try {
            System.out.print("Enter Book ID: ");
            int id = sc.nextInt();
            sc.nextLine();

            System.out.print("Enter Book Title: ");
            String title = sc.nextLine();

            System.out.print("Enter Author Name: ");
            String author = sc.nextLine();

            System.out.print("Enter Quantity: ");
            int qty = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO books(bid, title, author, quantity) VALUES(?,?,?,?)");

            ps.setInt(1, id);
            ps.setString(2, title);
            ps.setString(3, author);
            ps.setInt(4, qty);

            ps.executeUpdate();
            System.out.println("Book Added Successfully!");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteBook() {
        try {
            System.out.print("Enter Book ID to Delete: ");
            int id = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("DELETE FROM books WHERE bid=?");

            ps.setInt(1, id);
            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Book Deleted!");
            else
                System.out.println("Invalid Book ID");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void viewBooks() {
        try {
            Connection con = DBConnection.getConnection();
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM books");

            System.out.println("--- All Books ---");
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

    public void updateBookQuantity() {
        try {
            System.out.print("Enter Book ID to Update Quantity: ");
            int id = sc.nextInt();
            sc.nextLine();

            System.out.print("Enter New Quantity: ");
            int qty = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE books SET quantity=? WHERE bid=?");

            ps.setInt(1, qty);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();

            if (rows > 0)
                System.out.println("Book Quantity Updated Successfully!");
            else
                System.out.println("Invalid Book ID!");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void searchBookById() {
        try {
            System.out.print("Enter Book ID: ");
            int id = sc.nextInt();
            sc.nextLine();

            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM books WHERE bid=?");

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("--- Book Found ---");
                System.out.println("Book ID: " + rs.getInt("bid"));
                System.out.println("Book Title: " + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("Quantity: " + rs.getInt("quantity"));
            }
            else {
                System.out.println("No book found with that ID!");
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

    public void viewZeroQuantityBooks() {
        try {
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM books WHERE quantity = 0");

            ResultSet rs = ps.executeQuery();

            System.out.println("--- Out of Stock Books ---");
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
                System.out.println("All books are in stock!");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void viewBorrowedBooks() {
        try {
            Connection con = DBConnection.getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT b.borrow_id, u.username, bk.title, bk.author, b.borrow_date, b.return_date " +
                            "FROM borrow b " + "JOIN users u ON b.user_id = u.user_id " + "JOIN books bk ON b.book_id = bk.bid");

            ResultSet rs = ps.executeQuery();

            System.out.println("--- Borrowed Books List ---");
            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.println("Borrow ID: " + rs.getInt("borrow_id"));
                System.out.println("User Name: " + rs.getString("username"));
                System.out.println("Book Title: " + rs.getString("title"));
                System.out.println("Author Name: " + rs.getString("author"));
                System.out.println("Borrow Date: " + rs.getDate("borrow_date"));
                if(rs.getDate("return_date") == null){
                    System.out.println("Book Not Returned!");
                }
                else{
                    System.out.println("Return Date: " + rs.getDate("return_date"));
                }
                System.out.println("......");
            }

            if (!found)
                System.out.println("No active borrow records found!");
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}