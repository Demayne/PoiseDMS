import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection class is used to establish a connection to the PoiseDMS database.
 */
public class DatabaseConnection {
    // Database URL and credentials
    private static final String URL = "jdbc:mysql://localhost:3306/PoiseDMS?useSSL=false&serverTimezone=UTC";
    private static final String USER = "user2"; // Replace with your MySQL username
    private static final String PASSWORD = "Gunnerforlife7*"; // Replace with your MySQL password

    /**
     * Establishes a connection to the database.
     *
     * @return Connection object for interacting with the database.
     * @throws SQLException If a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish and return the database connection
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            // Handle case where JDBC driver is not found
            System.err.println("MySQL JDBC Driver not found: " + e.getMessage());
            throw new SQLException("Driver not found.", e);
        } catch (SQLException e) {
            // Handle SQL exceptions (e.g., invalid credentials or connection issues)
            System.err.println("Connection failed: " + e.getMessage());
            throw e;
        }
    }
}
