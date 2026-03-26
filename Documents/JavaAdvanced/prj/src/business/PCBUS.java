package business;

import entity.PC;
import util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class PCBUS {

    // Thêm PC
    public void addPC(PC pc){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql="INSERT INTO pcs(pc_name,status,price_per_hour) VALUES(?,?,?)";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1,pc.getPcName());
            ps.setString(2,pc.getStatus());
            ps.setDouble(3,pc.getPricePerHour());

            ps.executeUpdate();

            System.out.println("Add PC success");

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Hiển thị danh sách PC
    public void showPC(){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql="SELECT * FROM pcs";

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);

            while(rs.next()){

                System.out.println(
                        rs.getInt("pc_id")+" - "+
                                rs.getString("pc_name")+" - "+
                                rs.getString("status")+" - "+
                                rs.getDouble("price_per_hour")
                );
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}