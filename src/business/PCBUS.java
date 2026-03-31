package business;

import entity.PC;
import entity.OrderDetail;
import util.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class PCBUS {

    public void addPC(PC pc) {
        addPC(pc.getName(), pc.getArea(), pc.getConfig(), pc.getPricePerHour());
    }

    public void addPC(String name, String area, String config, double price) {

        try {
            if (name == null || name.trim().isEmpty()) {
                System.out.println("Tên PC không được để trống!");
                return;
            }

            if (area == null || area.trim().isEmpty()) {
                System.out.println("Khu vực không được để trống!");
                return;
            }

            if (config == null || config.trim().isEmpty()) {
                System.out.println("Cấu hình không được để trống!");
                return;
            }

            if (price <= 0) {
                System.out.println("Giá giờ phải lớn hơn 0!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            String checkSql = "SELECT * FROM pcs WHERE pc_name=?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, name.trim());
            ResultSet checkRs = checkPs.executeQuery();

            if (checkRs.next()) {
                System.out.println("Tên PC đã tồn tại!");
                return;
            }

            String sql = "INSERT INTO pcs(pc_name,area,config,status,price_per_hour) VALUES(?,?,?,?,?)";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name.trim());
            ps.setString(2, area.trim());
            ps.setString(3, config.trim());
            ps.setString(4, "AVAILABLE");
            ps.setDouble(5, price);

            ps.executeUpdate();

            System.out.println("Thêm PC thành công!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showPC() {

        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT * FROM pcs ORDER BY area ";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== DANH SÁCH PC =====");
            System.out.printf("%-5s %-15s %-12s %-25s %-15s %-10s\n",
                    "ID", "Tên PC", "Khu vực", "Cấu hình", "Trạng thái", "Giá");
            System.out.println("-".repeat(90));

            while (rs.next()) {
                System.out.printf("%-5d %-15s %-12s %-25s %-15s %-10.0f\n",
                        rs.getInt("pc_id"),
                        rs.getString("pc_name"),
                        rs.getString("area"),
                        rs.getString("config"),
                        rs.getString("status"),
                        rs.getDouble("price_per_hour")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deletePC(int pcId) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql = "DELETE FROM pcs WHERE pc_id=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pcId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Xóa PC thành công!");
            } else {
                System.out.println("Không tìm thấy PC!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatePC(int pcId, String name, String area, String config, double price) {

        try {

            if (name == null || name.trim().isEmpty()) {
                System.out.println("Tên PC không được để trống!");
                return;
            }

            if (area == null || area.trim().isEmpty()) {
                System.out.println("Khu vực không được để trống!");
                return;
            }

            if (config == null || config.trim().isEmpty()) {
                System.out.println("Cấu hình không được để trống!");
                return;
            }

            if (price <= 0) {
                System.out.println("Giá giờ phải lớn hơn 0!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            String checkSQL = "SELECT * FROM pcs WHERE pc_id=?";
            PreparedStatement checkPs = conn.prepareStatement(checkSQL);
            checkPs.setInt(1, pcId);
            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {
                System.out.println("Không tìm thấy PC!");
                return;
            }

            System.out.println("Thông tin cũ:");
            System.out.println("Tên PC: " + rs.getString("pc_name"));
            System.out.println("Khu vực: " + rs.getString("area"));
            System.out.println("Cấu hình: " + rs.getString("config"));
            System.out.println("Trạng thái: " + rs.getString("status"));
            System.out.println("Giá giờ: " + rs.getDouble("price_per_hour"));

            String sql = "UPDATE pcs SET pc_name=?, area=?, config=?, price_per_hour=? WHERE pc_id=?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, name.trim());
            ps.setString(2, area.trim());
            ps.setString(3, config.trim());
            ps.setDouble(4, price);
            ps.setInt(5, pcId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Cập nhật PC thành công!");
            } else {
                System.out.println("Cập nhật thất bại!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateStatus(int id, String status) {

        if (!status.equals("AVAILABLE") &&
                !status.equals("BUSY") &&
                !status.equals("MAINTENANCE")) {

            System.out.println("Trạng thái không hợp lệ!");
            return;
        }

        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql = "UPDATE pcs SET status=? WHERE pc_id=?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Cập nhật trạng thái thành công!");
            } else {
                System.out.println("Không tìm thấy PC!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void countPCByStatus() {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT status,COUNT(*) as total FROM pcs GROUP BY status";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== THỐNG KÊ PC =====");

            while (rs.next()) {
                System.out.println(rs.getString("status") + " : " + rs.getInt("total"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showAvailablePCs() {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT * FROM pcs WHERE status='AVAILABLE' ORDER BY area, pc_name";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== DANH SÁCH PC TRỐNG THEO KHU VỰC =====");

            String currentArea = "";
            int count = 0;

            while (rs.next()) {

                String area = rs.getString("area");

                if (!area.equals(currentArea)) {
                    currentArea = area;
                    System.out.println("\n--- " + currentArea.toUpperCase() + " ---");
                    System.out.printf("%-5s %-15s %-25s %-12s %-10s\n",
                            "ID", "Tên PC", "Cấu hình", "Trạng thái", "Giá/giờ");
                    System.out.println("-".repeat(75));
                }

                System.out.printf("%-5d %-15s %-25s %-12s %-10.0f\n",
                        rs.getInt("pc_id"),
                        rs.getString("pc_name"),
                        rs.getString("config"),
                        rs.getString("status"),
                        rs.getDouble("price_per_hour")
                );
                count++;
            }

            if (count == 0) {
                System.out.println("Không có PC nào trống!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPCAvailableForTime(int pcId, Timestamp startTime, Timestamp endTime) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT COUNT(*) as count FROM bookings " +
                    "WHERE pc_id=? AND status IN ('PENDING','CONFIRMED','SERVING') " +
                    "AND start_time < ? AND end_time > ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, pcId);
            ps.setTimestamp(2, endTime);
            ps.setTimestamp(3, startTime);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") == 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public int bookPC(int userId, int pcId, Timestamp startTime, Timestamp endTime) {

        try {
            String checkSQL = "SELECT price_per_hour FROM pcs WHERE pc_id=?";
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(checkSQL);
            ps.setInt(1, pcId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("PC không tồn tại!");
                return -1;
            }

            double pricePerHour = rs.getDouble("price_per_hour");

            if (startTime.getTime() >= endTime.getTime()) {
                System.out.println("Thời gian kết thúc phải lớn hơn thời gian bắt đầu!");
                return -1;
            }

            if (!isPCAvailableForTime(pcId, startTime, endTime)) {
                System.out.println("PC này đã được đặt trong khoảng thời gian này!");
                return -1;
            }

            long diff = endTime.getTime() - startTime.getTime();
            long hours = (long) Math.ceil(diff / (1000.0 * 60 * 60));
            double totalCost = hours * pricePerHour;

            String insertSQL = "INSERT INTO bookings(user_id,pc_id,start_time,end_time,status,total_cost) VALUES(?,?,?,?,?,?)";
            ps = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, userId);
            ps.setInt(2, pcId);
            ps.setTimestamp(3, startTime);
            ps.setTimestamp(4, endTime);
            ps.setString(5, "PENDING");
            ps.setDouble(6, totalCost);

            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            int bookingId = -1;
            if (generatedKeys.next()) {
                bookingId = generatedKeys.getInt(1);
            }

            System.out.println("Đặt PC thành công! (Booking ID: " + bookingId + ")");
            System.out.println("  - Thời gian: " + hours + " giờ");
            System.out.println("  - Chi phí: " + totalCost + " VND");
            System.out.println("  - Trạng thái: PENDING");

            return bookingId;

        } catch (Exception e) {
            System.out.println("Lỗi khi đặt PC!");
            e.printStackTrace();
        }

        return -1;
    }

    public void addFoodToBooking(int bookingId, List<OrderDetail> foodItems){

        try{
            Connection conn = DatabaseConnection.getConnection();

            String insertSQL = "INSERT INTO booking_details(booking_id,food_id,quantity,price,total_price) VALUES(?,?,?,?,?)";
            String checkStockSQL = "SELECT stock FROM foods WHERE food_id=?";
            String updateStockSQL = "UPDATE foods SET stock = stock - ? WHERE food_id=?";

            PreparedStatement insertPS = conn.prepareStatement(insertSQL);
            PreparedStatement checkPS = conn.prepareStatement(checkStockSQL);
            PreparedStatement stockPS = conn.prepareStatement(updateStockSQL);

            double foodTotal = 0;

            for(OrderDetail item : foodItems){

                checkPS.setInt(1,item.getFoodId());
                ResultSet rs = checkPS.executeQuery();

                if(rs.next()){
                    int stock = rs.getInt("stock");

                    if(stock < item.getQuantity()){
                        System.out.println("Không đủ hàng!");
                        return;
                    }
                } else {
                    System.out.println("Không tìm thấy dịch vụ!");
                    return;
                }

                stockPS.setInt(1,item.getQuantity());
                stockPS.setInt(2,item.getFoodId());
                stockPS.executeUpdate();

                insertPS.setInt(1,bookingId);
                insertPS.setInt(2,item.getFoodId());
                insertPS.setInt(3,item.getQuantity());
                insertPS.setDouble(4,item.getPrice());
                insertPS.setDouble(5,item.getTotalPrice());
                insertPS.addBatch();

                foodTotal += item.getTotalPrice();
            }

            insertPS.executeBatch();

            String updateBooking = "UPDATE bookings SET total_cost = total_cost + ? WHERE booking_id=?";
            PreparedStatement ps = conn.prepareStatement(updateBooking);
            ps.setDouble(1,foodTotal);
            ps.setInt(2,bookingId);
            ps.executeUpdate();

            System.out.println("Order dịch vụ thành công!");

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void printBookingInvoice(int bookingId){

        try{
            Connection conn = DatabaseConnection.getConnection();

            String bookingSQL = "SELECT b.booking_id, u.username, p.pc_name, b.start_time, b.end_time, b.total_cost, b.status " +
                    "FROM bookings b " +
                    "JOIN users u ON b.user_id=u.user_id " +
                    "JOIN pcs p ON b.pc_id=p.pc_id " +
                    "WHERE b.booking_id=?";

            PreparedStatement ps = conn.prepareStatement(bookingSQL);
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();

            if(!rs.next()){
                System.out.println("Không tìm thấy booking!");
                return;
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("                    HÓA ĐƠN PHÒNG NET");
            System.out.println("=".repeat(60));
            System.out.println("Mã đơn: " + rs.getInt("booking_id"));
            System.out.println("Khách hàng: " + rs.getString("username"));
            System.out.println("Máy: " + rs.getString("pc_name"));
            System.out.println("Thời gian bắt đầu: " + rs.getTimestamp("start_time"));
            System.out.println("Thời gian kết thúc: " + rs.getTimestamp("end_time"));
            System.out.println("-".repeat(60));

            double totalCost = rs.getDouble("total_cost");
            String bookingStatus = rs.getString("status");

            String detailsSQL = "SELECT bd.food_id, f.name, bd.quantity, bd.price, bd.total_price " +
                    "FROM booking_details bd " +
                    "JOIN foods f ON bd.food_id=f.food_id " +
                    "WHERE bd.booking_id=?";

            ps = conn.prepareStatement(detailsSQL);
            ps.setInt(1, bookingId);
            rs = ps.executeQuery();

            System.out.println(String.format("%-5s %-30s %-8s %-12s %-12s",
                    "ID", "Dịch vụ", "Số lượng", "Giá/cái", "Thành tiền"));
            System.out.println("-".repeat(60));

            while(rs.next()){
                System.out.println(String.format("%-5d %-30s %-8d %-12.0f %-12.0f",
                        rs.getInt("food_id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getDouble("total_price")
                ));
            }

            System.out.println("-".repeat(60));
            System.out.println("TỔNG CỘNG: " + String.format("%.0f", totalCost) + " VND");
            System.out.println("=".repeat(60));
            System.out.println("Trạng thái: " + bookingStatus);
            System.out.println("=".repeat(60));

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void showPendingRequestsAndOrders() {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql =
                    "SELECT b.booking_id, u.username, p.pc_name, b.start_time, b.end_time, " +
                            "b.status, b.total_cost, COUNT(bd.detail_id) AS food_count " +
                            "FROM bookings b " +
                            "JOIN users u ON b.user_id = u.user_id " +
                            "JOIN pcs p ON b.pc_id = p.pc_id " +
                            "LEFT JOIN booking_details bd ON b.booking_id = bd.booking_id " +
                            "WHERE b.status IN ('PENDING','CONFIRMED','SERVING') " +
                            "GROUP BY b.booking_id, u.username, p.pc_name, b.start_time, b.end_time, b.status, b.total_cost " +
                            "ORDER BY b.booking_id";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== DANH SÁCH YÊU CẦU ĐẶT MÁY & F&B =====");
            System.out.printf("%-5s %-15s %-10s %-20s %-20s %-12s %-8s %-12s\n",
                    "ID", "Khách", "PC", "Bắt đầu", "Kết thúc", "Trạng thái", "SL món", "Tổng tiền");
            System.out.println("-".repeat(120));

            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-15s %-10s %-20s %-20s %-12s %-8d %-12.0f\n",
                        rs.getInt("booking_id"),
                        rs.getString("username"),
                        rs.getString("pc_name"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getString("status"),
                        rs.getInt("food_count"),
                        rs.getDouble("total_cost"));
            }

            if (!found) {
                System.out.println("Không có yêu cầu nào đang chờ!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showBookingDetails(int bookingId) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql =
                    "SELECT b.booking_id, u.username, p.pc_name, b.start_time, b.end_time, b.status, b.total_cost " +
                            "FROM bookings b " +
                            "JOIN users u ON b.user_id = u.user_id " +
                            "JOIN pcs p ON b.pc_id = p.pc_id " +
                            "WHERE b.booking_id=?";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("Không tìm thấy booking!");
                return;
            }

            System.out.println("\n===== CHI TIẾT BOOKING =====");
            System.out.println("Booking ID: " + rs.getInt("booking_id"));
            System.out.println("Khách hàng: " + rs.getString("username"));
            System.out.println("Máy: " + rs.getString("pc_name"));
            System.out.println("Bắt đầu: " + rs.getTimestamp("start_time"));
            System.out.println("Kết thúc: " + rs.getTimestamp("end_time"));
            System.out.println("Trạng thái: " + rs.getString("status"));
            System.out.println("Tổng tiền: " + rs.getDouble("total_cost"));

            String foodSql =
                    "SELECT f.name, bd.quantity, bd.price, bd.total_price " +
                            "FROM booking_details bd " +
                            "JOIN foods f ON bd.food_id = f.food_id " +
                            "WHERE bd.booking_id=?";

            ps = conn.prepareStatement(foodSql);
            ps.setInt(1, bookingId);
            rs = ps.executeQuery();

            System.out.println("\n--- DỊCH VỤ F&B ---");
            boolean hasFood = false;

            while (rs.next()) {
                hasFood = true;
                System.out.printf("%-20s | SL: %-3d | Giá: %-10.0f | Thành tiền: %-10.0f\n",
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getDouble("total_price"));
            }

            if (!hasFood) {
                System.out.println("Không có món nào.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateBookingProgress(int bookingId, String newStatus) {

        try {
            newStatus = newStatus.toUpperCase();

            if (!newStatus.equals("PENDING") &&
                    !newStatus.equals("CONFIRMED") &&
                    !newStatus.equals("SERVING") &&
                    !newStatus.equals("COMPLETED")) {

                System.out.println("Trạng thái không hợp lệ!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            String checkSql = "SELECT status, pc_id FROM bookings WHERE booking_id=?";
            PreparedStatement ps = conn.prepareStatement(checkSql);
            ps.setInt(1, bookingId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                System.out.println("Không tìm thấy booking!");
                return;
            }

            String currentStatus = rs.getString("status");
            int pcId = rs.getInt("pc_id");

            boolean valid =
                    (currentStatus.equals("PENDING") && newStatus.equals("CONFIRMED")) ||
                            (currentStatus.equals("CONFIRMED") && newStatus.equals("SERVING")) ||
                            (currentStatus.equals("SERVING") && newStatus.equals("COMPLETED"));

            if (!valid) {
                System.out.println("Chỉ được cập nhật theo thứ tự: PENDING -> CONFIRMED -> SERVING -> COMPLETED");
                return;
            }

            String updateSql = "UPDATE bookings SET status=? WHERE booking_id=?";
            ps = conn.prepareStatement(updateSql);
            ps.setString(1, newStatus);
            ps.setInt(2, bookingId);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                if (newStatus.equals("SERVING")) {
                    String updatePcSql = "UPDATE pcs SET status='BUSY' WHERE pc_id=?";
                    PreparedStatement ps2 = conn.prepareStatement(updatePcSql);
                    ps2.setInt(1, pcId);
                    ps2.executeUpdate();
                }

                if (newStatus.equals("COMPLETED")) {
                    String updatePcSql = "UPDATE pcs SET status='AVAILABLE' WHERE pc_id=?";
                    PreparedStatement ps2 = conn.prepareStatement(updatePcSql);
                    ps2.setInt(1, pcId);
                    ps2.executeUpdate();
                }

                System.out.println("Cập nhật trạng thái thành công!");
            } else {
                System.out.println("Cập nhật thất bại!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showCustomerBookingStatus(String username) {

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql =
                    "SELECT b.booking_id, p.pc_name, b.start_time, b.end_time, b.status, b.total_cost " +
                            "FROM bookings b " +
                            "JOIN users u ON b.user_id = u.user_id " +
                            "JOIN pcs p ON b.pc_id = p.pc_id " +
                            "WHERE u.username=? ORDER BY b.booking_id DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n===== TRẠNG THÁI ĐƠN HÀNG CỦA BẠN =====");
            System.out.printf("%-5s %-10s %-20s %-20s %-12s %-12s\n",
                    "ID", "PC", "Bắt đầu", "Kết thúc", "Trạng thái", "Tổng tiền");
            System.out.println("-".repeat(95));

            boolean found = false;

            while (rs.next()) {
                found = true;
                System.out.printf("%-5d %-10s %-20s %-20s %-12s %-12.0f\n",
                        rs.getInt("booking_id"),
                        rs.getString("pc_name"),
                        rs.getTimestamp("start_time"),
                        rs.getTimestamp("end_time"),
                        rs.getString("status"),
                        rs.getDouble("total_cost"));
            }

            if (!found) {
                System.out.println("Bạn chưa có đơn nào.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}