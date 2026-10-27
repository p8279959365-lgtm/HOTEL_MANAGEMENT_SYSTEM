package hotelos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBConnection {
    private static final String URL = System.getenv().getOrDefault(
            "HOTEL_DB_URL",
            "jdbc:mysql://localhost:3306/HOTEL_MANAGEMENT_SYSTEM?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
    );
    private static final String USER = System.getenv().getOrDefault("HOTEL_DB_USER", "root");
    private static final String PASSWORD = System.getenv().getOrDefault("HOTEL_DB_PASSWORD", "");

    private DBConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
