package business;

import entity.Food;
import util.DatabaseConnection;

import java.sql.*;

public class FoodBUS {

    public void addFood(Food food){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql="INSERT INTO foods(name,price,stock,description) VALUES(?,?,?,?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1,food.getName());
            ps.setDouble(2,food.getPrice());
            ps.setInt(3,food.getStock());
            ps.setString(4,food.getDescription());

            ps.executeUpdate();

            System.out.println("Thêm dịch vụ thành công!");

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void showFoods(){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql="SELECT * FROM foods";

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== DANH SÁCH DỊCH VỤ =====");

            while(rs.next()){

                System.out.println(
                        rs.getInt("food_id")+" | "+
                                rs.getString("name")+" | "+
                                rs.getDouble("price")+" | "+
                                rs.getInt("stock")
                );
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}