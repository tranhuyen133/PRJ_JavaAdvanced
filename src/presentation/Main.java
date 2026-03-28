package presentation;

import business.UserBUS;
import business.PCBUS;
import business.FoodBUS;
import entity.Food;
import entity.User;
import entity.PC;

import java.util.Scanner;

public class Main {

    static Scanner sc = new Scanner(System.in);
    static UserBUS userBUS = new UserBUS();
    static PCBUS pcBUS = new PCBUS();
    static FoodBUS foodBUS = new FoodBUS();

    public static void main(String[] args) {

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

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            } catch (Exception e) {
                System.out.println("Vui lòng nhập số!");
            }
        }
    }

    // ========================
    // ĐĂNG KÝ
    // ========================

    public static void register() {

        System.out.println("\n===== ĐĂNG KÝ TÀI KHOẢN =====");

        System.out.print("Nhập username: ");
        String username = sc.nextLine();

        System.out.print("Nhập password: ");
        String password = sc.nextLine();

        User user = new User(username, password, "CUSTOMER");

        userBUS.register(user);
    }

    // ========================
    // ĐĂNG NHẬP
    // ========================

    public static void login() {

        System.out.println("\n===== ĐĂNG NHẬP =====");

        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        String role = userBUS.login(username, password);

        if (role == null) {

            System.out.println("Đăng nhập thất bại!");

        } else {

            System.out.println("Đăng nhập thành công!");

            if(role.equalsIgnoreCase("ADMIN")){
                adminMenu();
            }
            else if(role.equalsIgnoreCase("STAFF")){
                staffMenu();
            }
            else{
                customerMenu();
            }
        }
    }

    // ========================
    // MENU ADMIN
    // ========================

    public static void adminMenu() {

        while (true) {

            System.out.println("\n===== MENU QUẢN TRỊ =====");
            System.out.println("1. Thêm máy PC");
            System.out.println("2. Hiển thị danh sách PC");
            System.out.println("3. Hiển thị danh sách người dùng");
            System.out.println("4. Cập nhật quyền người dùng");
            System.out.println("5. Thêm dịch vụ");
            System.out.println("6. Hiển thị dịch vụ");
            System.out.println("0. Đăng xuất");

            System.out.print("Lựa chọn của bạn: ");

            try {

                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {

                    case 1:

                        System.out.print("Nhập tên PC: ");
                        String name = sc.nextLine();

                        String status = "AVAILABLE";

                        System.out.print("Nhập giá mỗi giờ: ");
                        double price = Double.parseDouble(sc.nextLine());

                        PC pc = new PC(name, status, price);

                        pcBUS.addPC(pc);

                        break;
                    case 2:
                        pcBUS.showPC();
                        break;

                    case 3:
                        userBUS.showUsers();
                        break;

                    case 4:

                        System.out.print("Nhập username cần đổi quyền: ");
                        String username = sc.nextLine();

                        System.out.print("Nhập quyền mới (ADMIN/STAFF/CUSTOMER): ");
                        String role = sc.nextLine();

                        userBUS.updateRole(username, role);

                        break;
                    case 5:

                        System.out.print("Tên dịch vụ: ");
                        String foodName = sc.nextLine();

                        System.out.print("Giá: ");
                        double foodPrice = Double.parseDouble(sc.nextLine());

                        System.out.print("Số lượng: ");
                        int stock = Integer.parseInt(sc.nextLine());

                        System.out.print("Mô tả: ");
                        String des = sc.nextLine();

                        Food food = new Food(foodName, foodPrice, stock, des);

                        foodBUS.addFood(food);

                        break;

                    case 6:
                        foodBUS.showFoods();
                        break;
                    case 0:
                        System.out.println("Đăng xuất");
                        return;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            } catch (Exception e) {
                System.out.println("Vui lòng nhập đúng định dạng!");
            }
        }
    }

    // ========================
    // MENU USER
    // ========================

    public static void customerMenu(){

        while(true){

            System.out.println("\n===== MENU KHÁCH HÀNG =====");
            System.out.println("1. Xem danh sách PC");
            System.out.println("2. Sử dụng PC");
            System.out.println("3. Xem menu đồ ăn");
            System.out.println("0. Đăng xuất");

            System.out.print("Lựa chọn của bạn: ");

            try{

                int choice = Integer.parseInt(sc.nextLine());

                switch(choice){

                    case 1:
                        pcBUS.showPC();
                        break;

                    case 2:

                        pcBUS.showPC();

                        System.out.print("Nhập ID PC muốn sử dụng: ");
                        int id = Integer.parseInt(sc.nextLine());

                        pcBUS.startPC(id);

                        break;

                    case 3:
                        foodBUS.showFoods();
                        break;

                    case 0:
                        System.out.println("Đăng xuất");
                        return;

                    default:
                        System.out.println("Lựa chọn không hợp lệ!");
                }

            }catch(Exception e){
                System.out.println("Vui lòng nhập số!");
            }
        }
    }
        public static void staffMenu(){

            while(true){

                System.out.println("\n===== MENU NHÂN VIÊN =====");
                System.out.println("1. Xem danh sách PC");
                System.out.println("2. Cập nhật trạng thái PC");
                System.out.println("3. Xem đơn dịch vụ");
                System.out.println("0. Đăng xuất");

                System.out.print("Lựa chọn: ");

                try{

                    int choice = Integer.parseInt(sc.nextLine());

                    switch(choice){

                        case 1:
                            pcBUS.showPC();
                            break;

                        case 2:

                            pcBUS.showPC();

                            System.out.print("Nhập ID PC: ");
                            int id = Integer.parseInt(sc.nextLine());

                            System.out.print("Trạng thái (AVAILABLE/BUSY/MAINTENANCE): ");
                            String status = sc.nextLine();

                            pcBUS.updateStatus(id,status);

                            break;

                        case 3:
                            foodBUS.showFoods();
                            break;

                        case 0:
                            System.out.println("Đăng xuất");
                            return;

                        default:
                            System.out.println("Lựa chọn không hợp lệ");
                    }

                }catch(Exception e){
                    System.out.println("Nhập sai định dạng!");
                }
            }
        }
    }
