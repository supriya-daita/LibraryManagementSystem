import java.sql.*;

public class DBConnection {

    private static final String url = "jdbc:mysql://localhost:3306/libraryRecords";
    private static final String user = "root";
    private static final String password = "your_password";

    public static Connection getConnection() {
        Connection con = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return con;
    }
}