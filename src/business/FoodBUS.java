package business;

import entity.Food;
import util.DatabaseConnection;

import java.sql.*;

public class FoodBUS {

    public void addFood(Food food) {

        try {

            if (food.getName() == null || food.getName().trim().isEmpty()) {
                System.out.println("Tên dịch vụ không được để trống!");
                return;
            }

            if (food.getPrice() <= 0) {
                System.out.println("Giá phải lớn hơn 0!");
                return;
            }

            if (food.getStock() < 0) {
                System.out.println("Số lượng tồn không được âm!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            String sql = "INSERT INTO foods(name,price,stock,description) VALUES(?,?,?,?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, food.getName().trim());
            ps.setDouble(2, food.getPrice());
            ps.setInt(3, food.getStock());
            ps.setString(4, food.getDescription());

            ps.executeUpdate();

            System.out.println("Thêm dịch vụ thành công!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showFoods() {

        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT * FROM foods ORDER BY food_id";

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== DANH SÁCH DỊCH VỤ =====");
            System.out.println(String.format("%-5s | %-20s | %-10s | %-8s | %-30s",
                    "ID", "Tên dịch vụ", "Giá", "Số lượng", "Mô tả"));
            System.out.println("-".repeat(90));

            while (rs.next()) {

                System.out.println(String.format("%-5d | %-20s | %-10.0f | %-8d | %-30s",
                        rs.getInt("food_id"),
                        rs.getString("name"),
                        rs.getDouble("price"),
                        rs.getInt("stock"),
                        rs.getString("description")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateFood(int foodId, double price, int stock, String description) {

        try {

            if (price <= 0) {
                System.out.println("Giá phải lớn hơn 0!");
                return;
            }

            if (stock < 0) {
                System.out.println("Số lượng tồn không được âm!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            String checkSQL = "SELECT * FROM foods WHERE food_id=?";
            PreparedStatement checkPs = conn.prepareStatement(checkSQL);
            checkPs.setInt(1, foodId);
            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {
                System.out.println("Không tìm thấy dịch vụ!");
                return;
            }

            System.out.println("Thông tin cũ:");
            System.out.println("Tên: " + rs.getString("name"));
            System.out.println("Giá: " + rs.getDouble("price"));
            System.out.println("Tồn kho: " + rs.getInt("stock"));
            System.out.println("Mô tả: " + rs.getString("description"));

            String sql = "UPDATE foods SET price=?, stock=?, description=? WHERE food_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, price);
            ps.setInt(2, stock);
            ps.setString(3, description);
            ps.setInt(4, foodId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Cập nhật dịch vụ thành công!");
            } else {
                System.out.println("Cập nhật thất bại!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFood(int foodId) {

        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "DELETE FROM foods WHERE food_id=?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1, foodId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Xóa dịch vụ thành công!");
            } else {
                System.out.println("Không tìm thấy dịch vụ!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}