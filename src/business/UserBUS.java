package business;

import entity.User;
import util.DatabaseConnection;

import java.sql.*;

public class UserBUS {

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

    // REGISTER
    public void register(User user){

        try{

            // kiểm tra username tồn tại
            if(checkUsernameExist(user.getUsername())){
                System.out.println(" Username đã tồn tại!");
                return;
            }

            Connection conn = DatabaseConnection.getConnection();

            String sql = "INSERT INTO users(username,password,role) VALUES(?,?,?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1,user.getUsername());
            ps.setString(2,user.getPassword());
            ps.setString(3,user.getRole());

            int rows = ps.executeUpdate();

            if(rows > 0){
                System.out.println(" Đăng ký thành công!");
            }else{
                System.out.println(" Đăng ký thất bại!");
            }

        }catch(Exception e){
            System.out.println("Lỗi khi đăng ký!");
            e.printStackTrace();
        }
    }

    // LOGIN
    public String login(String username,String password){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT role FROM users WHERE username=? AND password=?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1,username);
            ps.setString(2,password);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){

                String role = rs.getString("role");

                return role.trim(); //  loại bỏ khoảng trắng
            }

        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    // SHOW USERS
    public void showUsers(){

        try{

            Connection conn=DatabaseConnection.getConnection();

            String sql="SELECT * FROM users";

            Statement st=conn.createStatement();

            ResultSet rs=st.executeQuery(sql);

            while(rs.next()){

                System.out.println(
                        rs.getInt("user_id")+" | "+
                                rs.getString("username")+" | "+
                                rs.getString("role")
                );
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // UPDATE ROLE
    public void updateRole(String username,String role){

        try{

            Connection conn=DatabaseConnection.getConnection();

            String sql="UPDATE users SET role=? WHERE username=?";

            PreparedStatement ps=conn.prepareStatement(sql);

            ps.setString(1,role);
            ps.setString(2,username);

            int rows=ps.executeUpdate();

            if(rows>0){
                System.out.println("Update role success!");
            }else{
                System.out.println("User not found!");
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}