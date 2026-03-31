package business;

import entity.User;
import util.DatabaseConnection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;

public class UserBUS {

    public String hashPassword(String rawPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(rawPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rawPassword;
    }

    public boolean checkUsernameExist(String username){
        try{
            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT * FROM users WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1,username);

            ResultSet rs = ps.executeQuery();
            return rs.next();

        }catch(Exception e){
            e.printStackTrace();
        }

        return false;
    }



    public int getUserIdByUsername(String username){
        try{
            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT user_id FROM users WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getInt("user_id");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    public double getBalance(String username){
        try{
            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT balance FROM users WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                return rs.getDouble("balance");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    public void showBalance(String username){
        double balance = getBalance(username);
        System.out.println("Số dư hiện tại: " + String.format("%.0f", balance) + " VND");
    }

    public void addTransaction(int userId, String type, double amount, String description){
        try{
            Connection conn = DatabaseConnection.getConnection();

            String sql = "INSERT INTO transactions(user_id,type,amount,description) VALUES(?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setString(4, description);

            ps.executeUpdate();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void depositMoney(String username, double amount){

        try{
            if(amount <= 0){
                System.out.println("Số tiền nạp phải lớn hơn 0!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            String sql = "UPDATE users SET balance = balance + ? WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setDouble(1, amount);
            ps.setString(2, username);

            int rows = ps.executeUpdate();

            if(rows > 0){
                int userId = getUserIdByUsername(username);
                addTransaction(userId, "DEPOSIT", amount, "Nạp tiền vào tài khoản");

                System.out.println("Nạp tiền thành công!");
                showBalance(username);
            }else{
                System.out.println("Không tìm thấy người dùng!");
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean deductBalance(String username, double amount, String description){

        try{
            if(amount <= 0){
                System.out.println("Số tiền trừ phải lớn hơn 0!");
                return false;
            }

            Connection conn = DatabaseConnection.getConnection();

            String checkSql = "SELECT balance FROM users WHERE username=?";
            PreparedStatement checkPs = conn.prepareStatement(checkSql);
            checkPs.setString(1, username);

            ResultSet rs = checkPs.executeQuery();

            if(!rs.next()){
                System.out.println("Không tìm thấy người dùng!");
                return false;
            }

            double balance = rs.getDouble("balance");

            if(balance < amount){
                System.out.println("Số dư không đủ!");
                System.out.println("Số dư hiện tại: " + String.format("%.0f", balance) + " VND");
                System.out.println("Số tiền cần thanh toán: " + String.format("%.0f", amount) + " VND");
                return false;
            }

            String updateSql = "UPDATE users SET balance = balance - ? WHERE username=?";
            PreparedStatement updatePs = conn.prepareStatement(updateSql);
            updatePs.setDouble(1, amount);
            updatePs.setString(2, username);

            int rows = updatePs.executeUpdate();

            if(rows > 0){
                int userId = getUserIdByUsername(username);
                addTransaction(userId, "WITHDRAW", amount, description);

                System.out.println("Thanh toán thành công!");
                showBalance(username);
                return true;
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return false;
    }

    public void showProfile(String username) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String sql = "SELECT username, full_name, phone, role, balance FROM users WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("\n===== HỒ SƠ CÁ NHÂN =====");
                System.out.println("Username: " + rs.getString("username"));
                System.out.println("Họ tên: " + rs.getString("full_name"));
                System.out.println("Số điện thoại: " + rs.getString("phone"));
                System.out.println("Vai trò: " + rs.getString("role"));
                System.out.println("Số dư: " + String.format("%.0f", rs.getDouble("balance")) + " VND");
            } else {
                System.out.println("Không tìm thấy hồ sơ!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateProfile(String username, String fullName, String phone) {
        try {
            if (fullName == null || fullName.trim().isEmpty()) {
                System.out.println("Họ tên không được để trống!");
                return;
            }

            if (phone == null || phone.trim().isEmpty()) {
                if (phone.length() > 10) {
                    System.out.println("Số điện thoại không được quá 10 ký tự!");
                    return;
                }else{

                }
                System.out.println("Số điện thoại không được để trống!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET full_name=?, phone=? WHERE username=?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, fullName.trim());
            ps.setString(2, phone.trim());
            ps.setString(3, username);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Cập nhật hồ sơ thành công!");
            } else {
                System.out.println("Không tìm thấy người dùng!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register(User user){

        try{

            String username = user.getUsername().trim();
            String password = user.getPassword().trim();
            String fullName = user.getFullName() == null ? "" : user.getFullName().trim();
            String phone = user.getPhone() == null ? "" : user.getPhone().trim();
            String role = user.getRole().trim().toUpperCase();

            if(username.isEmpty()){
                System.out.println("Username không được để trống!");
                return;
            }

            if(username.contains(" ")){
                System.out.println("Username không được chứa khoảng trắng!");
                return;
            }

            if(username.length() < 3 || username.length() > 20){
                System.out.println("Username phải từ 3 - 20 ký tự!");
                return;
            }

            if(password.isEmpty()){
                System.out.println("Password không được để trống!");
                return;
            }

            if(password.length() < 6){
                System.out.println("Password phải ít nhất 6 ký tự!");
                return;
            }

            if(fullName.isEmpty()){
                System.out.println("Họ tên không được để trống!");
                return;
            }

            if(phone.isEmpty()){
                System.out.println("Số điện thoại không được để trống!");
                return;
            }

            if(!role.equals("ADMIN") && !role.equals("STAFF") && !role.equals("CUSTOMER")){
                System.out.println("Role không hợp lệ!");
                return;
            }

            if(checkUsernameExist(username)){
                System.out.println("Username đã tồn tại!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            String sql = "INSERT INTO users(username,password,full_name,phone,role,balance) VALUES(?,?,?,?,?,?)";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, username);
            ps.setString(2, hashPassword(password));
            ps.setString(3, fullName);
            ps.setString(4, phone);
            ps.setString(5, role);
            ps.setDouble(6, 0);

            int rows = ps.executeUpdate();

            if(rows > 0){
                System.out.println("Đăng ký thành công!");
            }else{
                System.out.println("Đăng ký thất bại!");
            }

        }catch(Exception e){
            System.out.println("Lỗi khi đăng ký!");
            e.printStackTrace();
        }
    }

    public String login(String username,String password){

        try{
            username = username.trim();
            password = password.trim();

            if(username.isEmpty() || password.isEmpty()){
                System.out.println("Username hoặc Password không được để trống!");
                return null;
            }

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT role FROM users WHERE username=? AND password=?";
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1,username);
            ps.setString(2,hashPassword(password));

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getString("role").trim();
            }else{
                System.out.println("Sai username hoặc password!");
            }

        }catch(Exception e){
            System.out.println("Lỗi khi đăng nhập!");
            e.printStackTrace();
        }

        return null;
    }

    public void showUsers(){

        try{
            Connection conn=DatabaseConnection.getConnection();

            String sql="SELECT * FROM users ORDER BY user_id";
            Statement st=conn.createStatement();
            ResultSet rs=st.executeQuery(sql);

            System.out.println("\n===== DANH SÁCH NGƯỜI DÙNG =====");
            System.out.println(String.format("%-5s | %-15s | %-20s | %-15s | %-10s | %-12s",
                    "ID", "Username", "Họ tên", "SĐT", "Quyền", "Balance"));
            System.out.println("-".repeat(95));

            while(rs.next()){
                System.out.println(String.format("%-5d | %-15s | %-20s | %-15s | %-10s | %-12.0f",
                        rs.getInt("user_id"),
                        rs.getString("username"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("role"),
                        rs.getDouble("balance")
                ));
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void updateRole(String username,String role){

        try{
            Connection conn=DatabaseConnection.getConnection();

            String sql="UPDATE users SET role=? WHERE username=?";
            PreparedStatement ps=conn.prepareStatement(sql);

            ps.setString(1,role.toUpperCase());
            ps.setString(2,username);

            int rows=ps.executeUpdate();

            if(rows>0){
                System.out.println("Cập nhật quyền thành công!");
            }else{
                System.out.println("Không tìm thấy người dùng!");
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void deleteUser(String username){

        try{
            Connection conn=DatabaseConnection.getConnection();

            String sql="DELETE FROM users WHERE username=?";
            PreparedStatement ps=conn.prepareStatement(sql);
            ps.setString(1,username);

            int rows=ps.executeUpdate();

            if(rows>0){
                System.out.println("Xóa người dùng thành công!");
            }else{
                System.out.println("Không tìm thấy người dùng!");
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}