package business;

import entity.PC;
import util.DatabaseConnection;

import java.sql.*;

public class PCBUS {

    public void addPC(PC pc){

        try{

            Connection conn=DatabaseConnection.getConnection();

            String sql="INSERT INTO pcs(pc_name,status,price_per_hour) VALUES(?,?,?)";

            PreparedStatement ps=conn.prepareStatement(sql);

            ps.setString(1,pc.getPcName());
            ps.setString(2,pc.getStatus());
            ps.setDouble(3,pc.getPricePerHour());

            ps.executeUpdate();

            System.out.println("Add PC success");

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void showPC(){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql = "SELECT * FROM pcs";

            Statement st = conn.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n===== DANH SÁCH PC =====");

            while(rs.next()){

                System.out.println(
                        "ID: " + rs.getInt("pc_id") +
                                " | Tên: " + rs.getString("pc_name") +
                                " | Trạng thái: " + rs.getString("status") +
                                " | Giá/giờ: " + rs.getDouble("price_per_hour")
                );
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void startPC(int pcId){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String checkSQL = "SELECT status FROM pcs WHERE pc_id=?";

            PreparedStatement ps = conn.prepareStatement(checkSQL);

            ps.setInt(1,pcId);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){

                String status = rs.getString("status");

                if(status.equalsIgnoreCase("BUSY")){
                    System.out.println("PC đang được sử dụng!");
                    return;
                }

                String updateSQL = "UPDATE pcs SET status='BUSY' WHERE pc_id=?";

                PreparedStatement ps2 = conn.prepareStatement(updateSQL);

                ps2.setInt(1,pcId);

                ps2.executeUpdate();

                System.out.println("Bắt đầu sử dụng PC thành công!");

            }else{
                System.out.println("Không tìm thấy PC!");
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void deletePC(int pcId){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql = "DELETE FROM pcs WHERE pc_id=?";

            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setInt(1,pcId);

            int rows = ps.executeUpdate();

            if(rows>0){
                System.out.println("Xóa PC thành công!");
            }else{
                System.out.println("Không tìm thấy PC!");
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void updatePC(int pcId,String name,double price){

        try{

            Connection conn = DatabaseConnection.getConnection();

            String sql="UPDATE pcs SET pc_name=?,price_per_hour=? WHERE pc_id=?";

            PreparedStatement ps=conn.prepareStatement(sql);

            ps.setString(1,name);
            ps.setDouble(2,price);
            ps.setInt(3,pcId);

            ps.executeUpdate();

            System.out.println("Cập nhật PC thành công!");

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void updateStatus(int id, String status) {
    }
}