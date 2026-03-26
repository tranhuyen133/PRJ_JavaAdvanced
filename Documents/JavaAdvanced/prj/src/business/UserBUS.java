package business;

import entity.User;
import util.DatabaseConnection;

import java.sql.*;

public class UserBUS {

    public void register(User user){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql="INSERT INTO users(username,password) VALUES(?,?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1,user.getUsername());
            ps.setString(2,user.getPassword());

            ps.executeUpdate();

            System.out.println("Register success");

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean login(String username,String password){

        try {

            Connection conn = DatabaseConnection.getConnection();

            String sql="SELECT * FROM users WHERE username=? AND password=?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1,username);
            ps.setString(2,password);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return true;
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        return false;
    }
}