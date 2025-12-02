import java.util.*;
import java.sql.*;

public class LibrarySystem {

    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        System.out.println("---------- Library Management System ----------");

        System.out.print("Username: ");
        String username = sc.nextLine();
        System.out.print("Password: ");
        String password = sc.nextLine();

        try{
            Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                int userId = rs.getInt("user_id");
                if (role.equals("admin")) {
                    adminMenu();
                }
                else {
                    userMenu(userId);
                }
            }
            else {
                System.out.println("Invalid Login!");
            }
        }
        catch(SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public static void adminMenu() {
        Scanner sc = new Scanner(System.in);
        AdminService admin = new AdminService();

        while (true) {
            System.out.println("------ Admin Menu ------");
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
            System.out.print("Choose: ");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> admin.addBook();
                case 2 -> admin.deleteBook();
                case 3 -> admin.viewBooks();
                case 4 -> admin.updateBookQuantity();
                case 5 -> admin.searchBookById();
                case 6 -> admin.searchBookByTitle();
                case 7 -> admin.searchBookByAuthor();
                case 8 -> admin.viewZeroQuantityBooks();
                case 9 -> admin.viewBorrowedBooks();
                case 10 -> { return; }
                default -> System.out.println("Invalid Choice!");
            }
        }
    }

    public static void userMenu(int userId) {
        Scanner sc = new Scanner(System.in);
        UserService user = new UserService(userId);

        while (true) {
            System.out.println("------ User Menu ------");
            System.out.println("1. View Books");
            System.out.println("2. Borrow Book");
            System.out.println("3. Return Book");
            System.out.println("4. Search Book by Title");
            System.out.println("5. Search Book by Author");
            System.out.println("6. Logout");
            System.out.print("Choose: ");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> user.viewAvailableBooks();
                case 2 -> user.borrowBook();
                case 3 -> user.returnBook();
                case 4 -> user.searchBookByTitle();
                case 5 -> user.searchBookByAuthor();
                case 6 -> { return; }
                default -> System.out.println("Invalid Choice!");
            }
        }
    }
}