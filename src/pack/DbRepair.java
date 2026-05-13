package pack;
import java.sql.*;

public class DbRepair {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:orderataxi.db");
            Statement st = conn.createStatement();
            try {
                st.execute("ALTER TABLE drivers ADD COLUMN avg_rating REAL DEFAULT 5.0");
                System.out.println("Column avg_rating added successfully!");
            } catch (SQLException e) {
                System.out.println("Column might already exist: " + e.getMessage());
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
