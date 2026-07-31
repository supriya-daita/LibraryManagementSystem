import java.sql.*;

/**
 * DBConnection.java
 * -----------------
 * Centralized utility class for establishing a connection to the MySQL database.
 * 
 * VULNERABILITY (CodeQL: java/hardcoded-credential-api-call):
 * Database credentials (username and password) are hardcoded directly into the source file.
 */
public class DBConnection {

    // INTENTIONAL VULNERABILITY: Hardcoded Database Credentials (CodeQL: java/hardcoded-credential-api-call)
    private static final String URL      = "jdbc:mysql://localhost:3306/libraryRecords";
    private static final String USER     = "root";
    private static final String PASSWORD = "AdminPassword123!_HardcodedSecret";

    public static Connection getConnection() {
        Connection con = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Hardcoded credential API call sink
            con = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection Error] Driver missing: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("[DBConnection Error] Connection failed: " + e.getMessage());
        }
        return con;
    }
}