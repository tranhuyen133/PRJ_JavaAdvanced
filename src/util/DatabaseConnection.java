package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:mysql://localhost:3306/cyber_management";
    private static final String USER = "root";
    private static final String PASSWORD = "Huyen1303";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            createTablesIfNotExist(conn);
            return conn;
        } catch (Exception e) {
            System.out.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private static void createTablesIfNotExist(Connection conn) {
        try {
            Statement stmt = conn.createStatement();

            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "username VARCHAR(50) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "full_name VARCHAR(100)," +
                    "phone VARCHAR(20)," +
                    "role VARCHAR(20) DEFAULT 'CUSTOMER'," +
                    "balance DECIMAL(10,2) DEFAULT 0.00," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createUsersTable);

            String createPCsTable = "CREATE TABLE IF NOT EXISTS pcs (" +
                    "pc_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "pc_name VARCHAR(50) NOT NULL UNIQUE," +
                    "area VARCHAR(50) NOT NULL DEFAULT 'Khu A'," +
                    "config VARCHAR(255) NOT NULL DEFAULT 'Standard'," +
                    "status VARCHAR(20) DEFAULT 'AVAILABLE'," +
                    "price_per_hour DOUBLE NOT NULL" +
                    ")";
            stmt.execute(createPCsTable);

            String createFoodsTable = "CREATE TABLE IF NOT EXISTS foods (" +
                    "food_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "name VARCHAR(100) NOT NULL," +
                    "price DECIMAL(10,2) NOT NULL," +
                    "stock INT DEFAULT 0," +
                    "description TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createFoodsTable);

            String createTransactionsTable = "CREATE TABLE IF NOT EXISTS transactions (" +
                    "transaction_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT," +
                    "type VARCHAR(20) NOT NULL," +
                    "amount DECIMAL(10,2) NOT NULL," +
                    "description TEXT," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                    ")";
            stmt.execute(createTransactionsTable);

            String createBookingsTable = "CREATE TABLE IF NOT EXISTS bookings (" +
                    "booking_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "user_id INT," +
                    "pc_id INT," +
                    "booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "start_time TIMESTAMP NOT NULL," +
                    "end_time TIMESTAMP NOT NULL," +
                    "status VARCHAR(20) DEFAULT 'PENDING'," +
                    "total_cost DECIMAL(10,2) DEFAULT 0.00," +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id)," +
                    "FOREIGN KEY (pc_id) REFERENCES pcs(pc_id)" +
                    ")";
            stmt.execute(createBookingsTable);

            String createBookingDetailsTable = "CREATE TABLE IF NOT EXISTS booking_details (" +
                    "detail_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "booking_id INT," +
                    "food_id INT," +
                    "quantity INT," +
                    "price DECIMAL(10,2)," +
                    "total_price DECIMAL(10,2)," +
                    "FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)," +
                    "FOREIGN KEY (food_id) REFERENCES foods(food_id)" +
                    ")";
            stmt.execute(createBookingDetailsTable);

        } catch (Exception e) {
            System.out.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
}