package restaurent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private final String url;
    private final String username;
    private final String password;

    public DBConnection() {
        this.url = "jdbc:postgresql://localhost:5432/mini_dish_db";
        this.username = "mini_dish_db_manager";
        this.password = "123456";
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
