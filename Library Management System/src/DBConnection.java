import java.sql.*;

/**
 * DBConnection.java
 * -----------------
 * Provides a centralized utility class for establishing a connection to the
 * MySQL database (libraryRecords). Any class requiring DB access calls
 * DBConnection.getConnection() to get a live Connection object.
 *
 * Database: libraryRecords
 * Tables  : users, books, borrow
 */
public class DBConnection {

    // ---------------------------------------------------------------
    // Database credentials and JDBC URL.
    // Update these values to match your local MySQL configuration.
    // ---------------------------------------------------------------
    private static final String URL      = "jdbc:mysql://localhost:3306/libraryRecords";
    private static final String USER     = "root";
    private static final String PASSWORD = "your_password";

    /**
     * Opens and returns a new Connection to the MySQL database.
     *
     * Steps performed:
     *  1. Loads the MySQL JDBC driver class into the JVM.
     *  2. Calls DriverManager.getConnection() with the configured credentials.
     *
     * @return A live {@link Connection} object on success, or {@code null}
     *         if the connection could not be established.
     *
     * @throws ClassNotFoundException if the MySQL JDBC driver JAR is missing
     *                                from the classpath (lib/ folder).
     * @throws SQLException           if the database URL, username, or
     *                                password is incorrect.
     */
    public static Connection getConnection() {
        Connection con = null;

        try {
            // Step 1: Load the MySQL Connector/J driver.
            // Required for older JDBC versions; modern drivers auto-register,
            // but this explicit call ensures compatibility.
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Step 2: Establish the connection using DriverManager.
            con = DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException e) {
            // Thrown when the MySQL JDBC driver JAR is not on the classpath.
            System.err.println("[DBConnection] JDBC Driver not found. "
                    + "Ensure mysql-connector-j.jar is in the lib/ folder.");
            System.err.println("  Cause: " + e.getMessage());

        } catch (SQLException e) {
            // Thrown when the DB is unreachable or credentials are wrong.
            System.err.println("[DBConnection] Failed to connect to database.");
            System.err.println("  SQL State : " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Message   : " + e.getMessage());
        }

        return con; // null if connection failed
    }
}