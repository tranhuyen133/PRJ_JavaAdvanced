package util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Thông tin kết nối database
    private static final String URL = "jdbc:mysql://localhost:3306/cyber_management";
    private static final String USER = "root";
    private static final String PASSWORD = "Huyen1303";

    // Hàm kết nối
    public static Connection getConnection() {

        Connection conn = null;

        try {

            // Load MySQL Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Tạo kết nối
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("Kết nối database thành công!");

        } catch (ClassNotFoundException e) {

            System.out.println("Không tìm thấy MySQL Driver");

        } catch (SQLException e) {

            System.out.println("Kết nối database thất bại");

        }

        return conn;
    }
}