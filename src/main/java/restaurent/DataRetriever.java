package restaurent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class DataRetriever {

     public Dish findDishById(Integer id) {
          String sql = "SELECT * FROM dish WHERE id = ?";
          DBConnection db = new DBConnection();

          try (Connection conn = db.getConnection();
               PreparedStatement ps = conn.prepareStatement(sql)) {

               ps.setInt(1, id);
               ResultSet rs = ps.executeQuery();

               if (rs.next()) {
                    return new Dish(
                            rs.getInt("id"),
                            rs.getString("name"),
                            DishTypeEnum.valueOf(rs.getString("dish_type")),
                            null // ingrédients récupérés plus tard
                    );
               }

          } catch (Exception e) {
               e.printStackTrace();
          }
          return null;
     }

     public List<Ingredient> findIngredients (int page, int size){
          string sql = "SELECT * "
     }
}
