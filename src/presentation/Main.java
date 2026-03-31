package presentation;

import business.UserBUS;
import business.PCBUS;
import business.FoodBUS;
import entity.Food;
import entity.User;
import entity.PC;
import entity.OrderDetail;
import util.DatabaseConnection;

import java.io.Console;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.text.SimpleDateFormat;

public class Main {

    static Scanner sc = new Scanner(System.in);
    static UserBUS userBUS = new UserBUS();
    static PCBUS pcBUS = new PCBUS();
    static FoodBUS foodBUS = new FoodBUS();
    static String currentUsername = "";
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String readPassword(String label) {
        Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword(label);
            return new String(pwd);
        } else {
            System.out.print(label);
            return sc.nextLine();
        }
    }

    public static void main(String[] args) {

        System.out.println("Starting Cyber Cafe Management System...");

        while (true) {

            System.out.println("\n===== HỆ THỐNG QUẢN LÝ PHÒNG NET =====");
            System.out.println("1. Đăng ký tài khoản");
            System.out.println("2. Đăng nhập");
            System.out.println("0. Thoát");

            System.out.print("Lựa chọn của bạn: ");

            try {

                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {

                    case 1:
                        register();
                        break;

                    case 2:
                        login();
                        break;

                    case 0:
                        System.out.println("Thoát chương trình...");
                        System.exit(0);
                        break;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            } catch (Exception e) {
                System.out.println("Vui lòng nhập số!");
            }
        }
    }

    public static void register() {

        System.out.println("\n===== ĐĂNG KÝ =====");

        System.out.print("Username: ");
        String username = sc.nextLine();

        String password = readPassword("Password: ");

        System.out.print("Họ tên: ");
        String fullName = sc.nextLine();

        System.out.print("Số điện thoại: ");
        String phone = sc.nextLine();

        User user = new User(username, password, fullName, phone, "CUSTOMER");
        userBUS.register(user);
    }

    public static void login() {

        while (true) {

            System.out.println("\n===== ĐĂNG NHẬP =====");

            System.out.print("Username: ");
            String username = sc.nextLine();

            String password = readPassword("Password: ");

            String role = userBUS.login(username, password);

            if (role == null) {

                System.out.print("Đăng nhập lại? (Y/N): ");
                String retry = sc.nextLine().trim().toUpperCase();

                if (!retry.equals("Y")) {
                    return;
                }

            } else {

                currentUsername = username;
                role = role.toUpperCase();

                if (role.equals("ADMIN")) {
                    adminMenu();
                } else if (role.equals("STAFF")) {
                    staffMenu();
                } else {
                    customerMenu();
                }
                return;
            }
        }
    }

    public static void adminMenu() {

        while (true) {

            System.out.println("\n===== ADMIN MENU =====");
            System.out.println("1. Quản lý PC");
            System.out.println("2. Quản lý dịch vụ");
            System.out.println("3. Quản lý User");
            System.out.println("0. Đăng xuất");

            try {
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {

                    case 1:
                        adminPCMenu();
                        break;

                    case 2:
                        adminServiceMenu();
                        break;

                    case 3:
                        adminUserMenu();
                        break;

                    case 0:
                        return;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            } catch (Exception e) {
                System.out.println("Vui lòng nhập số hợp lệ!");
            }
        }
    }

    public static void adminPCMenu() {

        while (true) {

            System.out.println("\n===== QUẢN LÝ PC =====");
            System.out.println("1. Thêm PC");
            System.out.println("2. Danh sách PC");
            System.out.println("3. Xóa PC");
            System.out.println("4. Sửa PC");
            System.out.println("0. Quay lại");

            try {
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {

                    case 1:
                        System.out.print("Tên PC: ");
                        String name = sc.nextLine();

                        System.out.print("Khu vực: ");
                        String area = sc.nextLine();

                        System.out.print("Cấu hình: ");
                        String config = sc.nextLine();

                        System.out.print("Giá giờ: ");
                        double price = Double.parseDouble(sc.nextLine());

                        PC pc = new PC(name, area, config, "AVAILABLE", price);
                        pcBUS.addPC(pc);
                        break;

                    case 2:
                        pcBUS.showPC();
                        break;

                    case 3:
                        pcBUS.showPC();

                        System.out.print("ID PC: ");
                        int id = Integer.parseInt(sc.nextLine());

                        System.out.print("Bạn chắc chắn muốn xóa PC này? (Y/N): ");
                        String confirmDelete = sc.nextLine().trim().toUpperCase();

                        if (confirmDelete.equals("Y")) {
                            pcBUS.deletePC(id);
                        } else {
                            System.out.println("Đã hủy xóa!");
                        }
                        break;

                    case 4:
                        pcBUS.showPC();

                        System.out.print("ID PC cần sửa: ");
                        int updateId = Integer.parseInt(sc.nextLine());

                        System.out.print("Tên PC mới: ");
                        String newName = sc.nextLine();

                        System.out.print("Khu vực mới: ");
                        String newArea = sc.nextLine();

                        System.out.print("Cấu hình mới: ");
                        String newConfig = sc.nextLine();

                        System.out.print("Giá giờ mới: ");
                        double newPrice = Double.parseDouble(sc.nextLine());

                        pcBUS.updatePC(updateId, newName, newArea, newConfig, newPrice);
                        break;

                    case 0:
                        return;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            } catch (Exception e) {
                System.out.println("Dữ liệu không hợp lệ!");
            }
        }
    }

    public static void adminServiceMenu() {

        while (true) {

            System.out.println("\n===== QUẢN LÝ DỊCH VỤ =====");
            System.out.println("1. Thêm dịch vụ");
            System.out.println("2. Danh sách dịch vụ");
            System.out.println("3. Xóa dịch vụ");
            System.out.println("4. Sửa dịch vụ");
            System.out.println("0. Quay lại");

            try {
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {

                    case 1:
                        System.out.print("Tên dịch vụ: ");
                        String fname = sc.nextLine();

                        System.out.print("Giá: ");
                        double fprice = Double.parseDouble(sc.nextLine());

                        System.out.print("Stock: ");
                        int stock = Integer.parseInt(sc.nextLine());

                        System.out.print("Mô tả: ");
                        String des = sc.nextLine();

                        Food food = new Food(fname, fprice, stock, des);
                        foodBUS.addFood(food);
                        break;

                    case 2:
                        foodBUS.showFoods();
                        break;

                    case 3:
                        foodBUS.showFoods();

                        System.out.print("ID dịch vụ: ");
                        int fid = Integer.parseInt(sc.nextLine());

                        System.out.print("Bạn chắc chắn muốn xóa dịch vụ này? (Y/N): ");
                        String confirmDelete = sc.nextLine().trim().toUpperCase();

                        if (confirmDelete.equals("Y")) {
                            foodBUS.deleteFood(fid);
                        } else {
                            System.out.println("Đã hủy xóa!");
                        }
                        break;

                    case 4:
                        foodBUS.showFoods();

                        System.out.print("ID dịch vụ cần sửa: ");
                        int foodId = Integer.parseInt(sc.nextLine());

                        System.out.print("Giá mới: ");
                        double newPrice = Double.parseDouble(sc.nextLine());

                        System.out.print("Stock mới: ");
                        int newStock = Integer.parseInt(sc.nextLine());

                        System.out.print("Mô tả mới: ");
                        String newDes = sc.nextLine();

                        foodBUS.updateFood(foodId, newPrice, newStock, newDes);
                        break;

                    case 0:
                        return;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            } catch (Exception e) {
                System.out.println("Dữ liệu không hợp lệ!");
            }
        }
    }

    public static void adminUserMenu() {

        while (true) {

            System.out.println("\n===== QUẢN LÝ USER =====");
            System.out.println("1. Danh sách User");
            System.out.println("2. Cập nhật Role");
            System.out.println("3. Xóa User");
            System.out.println("0. Quay lại");

            try {
                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {

                    case 1:
                        userBUS.showUsers();
                        break;

                    case 2:
                        System.out.print("Username: ");
                        String username = sc.nextLine();

                        System.out.print("Role (ADMIN/STAFF/CUSTOMER): ");
                        String role = sc.nextLine();

                        userBUS.updateRole(username, role);
                        break;

                    case 3:
                        System.out.print("Username: ");
                        String u = sc.nextLine();
                        userBUS.deleteUser(u);
                        break;

                    case 0:
                        return;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            } catch (Exception e) {
                System.out.println("Dữ liệu không hợp lệ!");
            }
        }
    }

    public static void customerMenu(){

        while(true){

            System.out.println("\n===== MENU KHÁCH =====");
            System.out.println("1. Xem PC");
            System.out.println("2. Xem PC trống");
            System.out.println("3. Đặt chỗ PC + Gọi món");
            System.out.println("4. Xem dịch vụ");
            System.out.println("5. Xem số dư");
            System.out.println("6. Nạp tiền");
            System.out.println("7. Tra cứu trạng thái đơn của tôi");
            System.out.println("8. Xem hồ sơ cá nhân");
            System.out.println("9. Cập nhật hồ sơ cá nhân");
            System.out.println("0. Đăng xuất");

            try{
                int choice = Integer.parseInt(sc.nextLine());

                switch(choice){

                    case 1:
                        pcBUS.showPC();
                        break;

                    case 2:
                        pcBUS.showAvailablePCs();
                        break;

                    case 3:
                        bookingMenu(currentUsername);
                        break;

                    case 4:
                        foodBUS.showFoods();
                        break;

                    case 5:
                        userBUS.showBalance(currentUsername);
                        break;

                    case 6:
                        System.out.print("Nhập số tiền muốn nạp: ");
                        double amount = Double.parseDouble(sc.nextLine());
                        userBUS.depositMoney(currentUsername, amount);
                        break;

                    case 7:
                        pcBUS.showCustomerBookingStatus(currentUsername);
                        break;

                    case 8:
                        userBUS.showProfile(currentUsername);
                        break;

                    case 9:
                        System.out.print("Họ tên mới: ");
                        String fullName = sc.nextLine();

                        System.out.print("Số điện thoại mới: ");
                        String phone = sc.nextLine();

                        userBUS.updateProfile(currentUsername, fullName, phone);
                        break;

                    case 0:
                        return;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            }catch(Exception e){
                System.out.println("Dữ liệu không hợp lệ!");
            }
        }
    }

    public static void staffMenu(){

        while(true){

            System.out.println("\n===== MENU STAFF =====");
            System.out.println("1. Danh sách PC");
            System.out.println("2. Cập nhật trạng thái PC");
            System.out.println("3. Xem dịch vụ");
            System.out.println("4. Báo cáo");
            System.out.println("5. Xem yêu cầu đặt máy & F&B");
            System.out.println("6. Cập nhật tiến độ đơn");
            System.out.println("0. Đăng xuất");

            try{
                int choice = Integer.parseInt(sc.nextLine());

                switch(choice){

                    case 1:
                        pcBUS.showPC();
                        break;

                    case 2:
                        pcBUS.showPC();

                        System.out.print("ID PC: ");
                        int id = Integer.parseInt(sc.nextLine());

                        System.out.println("1. AVAILABLE");
                        System.out.println("2. BUSY");
                        System.out.println("3. MAINTENANCE");
                        System.out.print("Chọn trạng thái: ");

                        int st = Integer.parseInt(sc.nextLine());

                        String status="AVAILABLE";
                        if(st==2) status="BUSY";
                        if(st==3) status="MAINTENANCE";

                        pcBUS.updateStatus(id,status);
                        break;

                    case 3:
                        foodBUS.showFoods();
                        break;

                    case 4:
                        showSystemReport();
                        break;

                    case 5:
                        pcBUS.showPendingRequestsAndOrders();

                        System.out.print("\nXem chi tiết booking? Nhập ID (0 để bỏ qua): ");
                        int bookingId = Integer.parseInt(sc.nextLine());
                        if(bookingId != 0){
                            pcBUS.showBookingDetails(bookingId);
                        }
                        break;

                    case 6:
                        pcBUS.showPendingRequestsAndOrders();

                        System.out.print("Nhập Booking ID cần cập nhật: ");
                        int updateBookingId = Integer.parseInt(sc.nextLine());

                        System.out.println("1. CONFIRMED");
                        System.out.println("2. SERVING");
                        System.out.println("3. COMPLETED");
                        System.out.print("Chọn trạng thái mới: ");
                        int progressChoice = Integer.parseInt(sc.nextLine());

                        String newStatus = "";
                        if(progressChoice == 1) newStatus = "CONFIRMED";
                        if(progressChoice == 2) newStatus = "SERVING";
                        if(progressChoice == 3) newStatus = "COMPLETED";

                        if(newStatus.isEmpty()){
                            System.out.println("Lựa chọn không hợp lệ!");
                        }else{
                            pcBUS.updateBookingProgress(updateBookingId, newStatus);
                        }
                        break;

                    case 0:
                        return;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            }catch(Exception e){
                System.out.println("Dữ liệu không hợp lệ!");
            }
        }
    }

    public static void showSystemReport() {
        pcBUS.countPCByStatus();

        try {
            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT DATE(created_at) d, SUM(amount) total FROM transactions GROUP BY d";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getDate("d") + " | " + rs.getDouble("total"));
            }

        } catch (Exception e) {
            System.out.println("Report error");
        }
    }

    public static void bookingMenu(String username){

        try{

            Connection conn = DatabaseConnection.getConnection();

            int userId = userBUS.getUserIdByUsername(username);

            if(userId == -1){
                System.out.println("Không tìm thấy người dùng!");
                return;
            }

            double balance = userBUS.getBalance(username);
            System.out.println("Số dư hiện tại: " + String.format("%.0f", balance) + " VND");

            pcBUS.showAvailablePCs();

            System.out.print("\nNhập ID PC: ");
            int pcId = Integer.parseInt(sc.nextLine());

            System.out.print("Nhập thời gian bắt đầu (dd/MM/yyyy HH:mm): ");
            String startTimeStr = sc.nextLine();

            System.out.print("Nhập thời gian kết thúc (dd/MM/yyyy HH:mm): ");
            String endTimeStr = sc.nextLine();

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            sdf.setLenient(false);

            Timestamp startTime = null;
            Timestamp endTime = null;

            try {
                startTime = new Timestamp(sdf.parse(startTimeStr).getTime());
                endTime = new Timestamp(sdf.parse(endTimeStr).getTime());
            } catch (ParseException e) {
                System.out.println("Sai định dạng! Ví dụ đúng: 31/03/2026 14:30");
                return;
            }

            if (startTime.getTime() < System.currentTimeMillis()) {
                System.out.println("Thời gian bắt đầu phải là thời điểm trong tương lai!");
                return;
            }
            if(startTime.getTime() >= endTime.getTime()){
                System.out.println("Thời gian kết thúc phải lớn hơn thời gian bắt đầu!");
                return;
            }

            String pcSql = "SELECT price_per_hour FROM pcs WHERE pc_id=?";
            PreparedStatement pcPs = conn.prepareStatement(pcSql);
            pcPs.setInt(1, pcId);
            ResultSet pcRs = pcPs.executeQuery();

            if(!pcRs.next()){
                System.out.println("PC không tồn tại!");
                return;
            }

            if(!pcBUS.isPCAvailableForTime(pcId, startTime, endTime)){
                System.out.println("PC này đã được đặt trong khoảng thời gian này!");
                return;
            }

            double pricePerHour = pcRs.getDouble("price_per_hour");
            long diff = endTime.getTime() - startTime.getTime();
            long hours = (long)Math.ceil(diff / (1000.0 * 60 * 60));
            double pcCost = hours * pricePerHour;

            if(pcCost > balance){
                System.out.println("Không đủ số dư để đặt máy!");
                System.out.println("Tiền máy: " + String.format("%.0f", pcCost) + " VND");
                System.out.println("Balance hiện tại: " + String.format("%.0f", balance) + " VND");
                System.out.println("Vui lòng nạp thêm tiền.");
                return;
            }

            List<OrderDetail> foodItems = new ArrayList<>();
            double foodTotal = 0;

            System.out.print("\nBạn có muốn gọi thêm F&B không? (Y/N): ");
            String choice = sc.nextLine().trim().toUpperCase();

            if(choice.equals("Y")){

                boolean addMore = true;

                while(addMore){

                    foodBUS.showFoods();

                    int foodId;

                    while(true){
                        try{
                            System.out.print("\nNhập ID dịch vụ (0 để kết thúc): ");
                            foodId = Integer.parseInt(sc.nextLine());
                            break;
                        }catch(NumberFormatException e){
                            System.out.println(" Vui lòng nhập số hợp lệ!");
                        }
                    }

                    if(foodId == 0) break;

                    String foodSQL = "SELECT name, price, stock FROM foods WHERE food_id=?";
                    PreparedStatement foodPs = conn.prepareStatement(foodSQL);
                    foodPs.setInt(1, foodId);
                    ResultSet foodRs = foodPs.executeQuery();

                    if(!foodRs.next()){
                        System.out.println("Dịch vụ không tồn tại!");
                        continue;
                    }

                    String foodName = foodRs.getString("name");
                    double foodPrice = foodRs.getDouble("price");
                    int foodStock = foodRs.getInt("stock");

                    System.out.print("Số lượng: ");
                    int quantity = Integer.parseInt(sc.nextLine());

                    if(quantity <= 0 || quantity > foodStock){
                        System.out.println("Số lượng không hợp lệ!");
                        continue;
                    }

                    double itemTotal = quantity * foodPrice;
                    double estimatedTotal = pcCost + foodTotal + itemTotal;

                    if(estimatedTotal > balance){
                        System.out.println("Không đủ số dư để thêm món này!");
                        System.out.println("Tổng tạm tính sau khi thêm món: " + String.format("%.0f", estimatedTotal) + " VND");
                        System.out.println("Balance hiện tại: " + String.format("%.0f", balance) + " VND");
                        continue;
                    }

                    OrderDetail item = new OrderDetail(foodId, foodName, quantity, foodPrice);
                    foodItems.add(item);
                    foodTotal += itemTotal;

                    System.out.println("✓ Thêm " + quantity + " " + foodName + " vào đơn");
                    System.out.println("Tổng tạm tính hiện tại: " + String.format("%.0f", (pcCost + foodTotal)) + " VND");

                    System.out.print("\nTiếp tục thêm? (Y/N): ");
                    String cont = sc.nextLine().trim().toUpperCase();
                    if(!cont.equals("Y")) addMore = false;
                }
            }

            double finalTotal = pcCost + foodTotal;

            if(finalTotal > balance){
                System.out.println("Không đủ số dư để thanh toán!");
                return;
            }

            int bookingId = pcBUS.bookPC(userId, pcId, startTime, endTime);

            if(bookingId <= 0){
                System.out.println("Không thể đặt PC!");
                return;
            }

            if(!foodItems.isEmpty()){
                pcBUS.addFoodToBooking(bookingId, foodItems);
            }

            boolean paid = userBUS.deductBalance(username, finalTotal, "Thanh toán booking ID = " + bookingId);

            if(!paid){
                System.out.println("Thanh toán thất bại vì số dư không đủ!");
                return;
            }

            System.out.println("\n===== THANH TOÁN THÀNH CÔNG =====");
            System.out.println("Tiền máy: " + String.format("%.0f", pcCost) + " VND");
            System.out.println("Tiền F&B: " + String.format("%.0f", foodTotal) + " VND");
            System.out.println("Tổng tiền: " + String.format("%.0f", finalTotal) + " VND");

            System.out.print("\nIn hóa đơn? (Y/N): ");
            String printChoice = sc.nextLine().trim().toUpperCase();
            if(printChoice.equals("Y")){
                pcBUS.printBookingInvoice(bookingId);
            }

        }catch(Exception e){
            System.out.println("Lỗi khi đặt chỗ!");
            e.printStackTrace();
        }
    }
}