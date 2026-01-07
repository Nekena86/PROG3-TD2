package restaurent;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
     DBConnection db = new DBConnection();

     public Dish findDishById(Integer id) {

          Dish dish = null;

          String dishSql = "SELECT id, name, dish_type FROM Dish WHERE id = ?";
          String ingSql = "SELECT id, name, price, category FROM Ingredient WHERE dish_id = ?";

          try (Connection conn = db.getConnection();
               PreparedStatement dishStmt = conn.prepareStatement(dishSql)) {

               dishStmt.setInt(1, id);
               ResultSet rsDish = dishStmt.executeQuery();

               if (rsDish.next()) {
                    dish = new Dish(
                            rsDish.getInt("id"),
                            rsDish.getString("name"),
                            rsDish.getString("dish_type"),
                            new ArrayList<>()
                    );
               }

               if (dish != null) {
                    try (PreparedStatement ingStmt = conn.prepareStatement(ingSql)) {
                         ingStmt.setInt(1, id);
                         ResultSet rsIng = ingStmt.executeQuery();

                         while (rsIng.next()) {
                              Ingredient ing = new Ingredient(
                                      rsIng.getInt("id"),
                                      rsIng.getString("name"),
                                      rsIng.getDouble("price"),
                                      CategoryEnum.valueOf(rsIng.getString("category")),
                                      dish
                              );
                              dish.getIngredients().add(ing);
                         }
                    }
               }

          } catch (SQLException e) {
               throw new RuntimeException("Erreur findDishById", e);
          }

          return dish;
     }


     public List<Ingredient> findIngredients(int page, int size) {

          List<Ingredient> ingredients = new ArrayList<>();
          int offset = (page - 1) * size;

          String sql = """
            SELECT i.id, i.name, i.price, i.category,
                   d.id AS dish_id, d.name AS dish_name, d.dish_type
            FROM Ingredient i
            JOIN Dish d ON i.dish_id = d.id
            ORDER BY i.id
            LIMIT ? OFFSET ?
        """;

          try (Connection conn = db.getConnection();
               PreparedStatement stmt = conn.prepareStatement(sql)) {

               stmt.setInt(1, size);
               stmt.setInt(2, offset);

               ResultSet rs = stmt.executeQuery();

               while (rs.next()) {
                    Dish dish = new Dish(
                            rs.getInt("dish_id"),
                            rs.getString("dish_name"),
                            rs.getString("dish_type")
                    );

                    Ingredient ing = new Ingredient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            CategoryEnum.valueOf(rs.getString("category")),
                            dish
                    );

                    ingredients.add(ing);
               }

          } catch (SQLException e) {
               throw new RuntimeException("Erreur pagination ingrédients", e);
          }

          return ingredients;
     }


     public List<Ingredient> createIngredient(List<Ingredient> newIngredients) {

          String checkSql = "SELECT id FROM Ingredient WHERE name = ?";
          String insertSql = """
            INSERT INTO Ingredient(name, price, category, dish_id)
            VALUES (?, ?, ?, ?)
        """;

          try (Connection conn = db.getConnection()) {

               conn.setAutoCommit(false);

               try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                    PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                    for (Ingredient ing : newIngredients) {

                         checkStmt.setString(1, ing.getName());
                         ResultSet rs = checkStmt.executeQuery();

                         if (rs.next()) {
                              throw new RuntimeException(
                                      "Ingrédient déjà existant : " + ing.getName()
                              );
                         }

                         insertStmt.setString(1, ing.getName());
                         insertStmt.setDouble(2, ing.getPrice());
                         insertStmt.setString(3, ing.getCategory().name());
                         insertStmt.setInt(4, ing.getDish().getId());
                         insertStmt.executeUpdate();
                    }

                    conn.commit();
                    return newIngredients;

               } catch (RuntimeException | SQLException e) {
                    conn.rollback();
                    throw new RuntimeException("Erreur création ingrédients", e);
               }

          } catch (SQLException e) {
               throw new RuntimeException("Erreur connexion DB", e);
          }
     }

     public Dish saveDish(Dish dishToSave) {

          String checkSql = "SELECT id FROM Dish WHERE id = ?";
          String insertSql = "INSERT INTO Dish(name, dish_type) VALUES (?, ?)";
          String updateSql = "UPDATE Dish SET name = ?, dish_type = ? WHERE id = ?";
          String clearIngredients = "UPDATE Ingredient SET dish_id = NULL WHERE dish_id = ?";
          String associateIngredient = "UPDATE Ingredient SET dish_id = ? WHERE id = ?";

          try (Connection conn = db.getConnection()) {

               conn.setAutoCommit(false);
               boolean exists;

               try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, dishToSave.getId());
                    exists = checkStmt.executeQuery().next();
               }

               if (!exists) {
                    try (PreparedStatement insertStmt =
                                 conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

                         insertStmt.setString(1, dishToSave.getName());
                         insertStmt.setString(2, dishToSave.getDishType());
                         insertStmt.executeUpdate();

                         ResultSet keys = insertStmt.getGeneratedKeys();
                         if (keys.next()) {
                              dishToSave.setId(keys.getInt(1));
                         }
                    }
               } else {
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                         updateStmt.setString(1, dishToSave.getName());
                         updateStmt.setString(2, dishToSave.getDishType());
                         updateStmt.setInt(3, dishToSave.getId());
                         updateStmt.executeUpdate();
                    }

                    try (PreparedStatement clearStmt = conn.prepareStatement(clearIngredients)) {
                         clearStmt.setInt(1, dishToSave.getId());
                         clearStmt.executeUpdate();
                    }
               }

               try (PreparedStatement assocStmt = conn.prepareStatement(associateIngredient)) {
                    for (Ingredient ing : dishToSave.getIngredients()) {
                         assocStmt.setInt(1, dishToSave.getId());
                         assocStmt.setInt(2, ing.getId());
                         assocStmt.executeUpdate();
                    }
               }

               conn.commit();
               return dishToSave;

          } catch (SQLException e) {
               throw new RuntimeException("Erreur saveDish", e);
          }
     }


     public List<Dish> findDishByIngredientName(String ingredientName) {

          List<Dish> dishes = new ArrayList<>();

          String sql = """
            SELECT DISTINCT d.id, d.name, d.dish_type
            FROM Dish d
            JOIN Ingredient i ON i.dish_id = d.id
            WHERE LOWER(i.name) LIKE ?
        """;

          try (Connection conn = db.getConnection();
               PreparedStatement stmt = conn.prepareStatement(sql)) {

               stmt.setString(1, "%" + ingredientName.toLowerCase() + "%");
               ResultSet rs = stmt.executeQuery();

               while (rs.next()) {
                    dishes.add(new Dish(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("dish_type")
                    ));
               }

          } catch (SQLException e) {
               throw new RuntimeException("Erreur findDishByIngredientName", e);
          }

          return dishes;
     }


     public List<Ingredient> findIngredientsByCriteria(
             String ingredientName,
             CategoryEnum category,
             String dishName,
             int page,
             int size) {

          List<Ingredient> ingredients = new ArrayList<>();
          List<Object> params = new ArrayList<>();

          StringBuilder sql = new StringBuilder("""
            SELECT i.id, i.name, i.price, i.category,
                   d.id AS dish_id, d.name AS dish_name, d.dish_type
            FROM Ingredient i
            JOIN Dish d ON i.dish_id = d.id
            WHERE 1=1
        """);

          if (ingredientName != null) {
               sql.append(" AND LOWER(i.name) LIKE ?");
               params.add("%" + ingredientName.toLowerCase() + "%");
          }

          if (category != null) {
               sql.append(" AND i.category = ?");
               params.add(category.name());
          }

          if (dishName != null) {
               sql.append(" AND LOWER(d.name) LIKE ?");
               params.add("%" + dishName.toLowerCase() + "%");
          }

          sql.append(" ORDER BY i.id LIMIT ? OFFSET ?");
          params.add(size);
          params.add((page - 1) * size);

          try (Connection conn = db.getConnection();
               PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

               for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
               }

               ResultSet rs = stmt.executeQuery();

               while (rs.next()) {
                    Dish dish = new Dish(
                            rs.getInt("dish_id"),
                            rs.getString("dish_name"),
                            rs.getString("dish_type")
                    );

                    Ingredient ing = new Ingredient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            CategoryEnum.valueOf(rs.getString("category")),
                            dish
                    );

                    ingredients.add(ing);
               }

          } catch (SQLException e) {
               throw new RuntimeException("Erreur findIngredientsByCriteria", e);
          }

          return ingredients;
     }
}
