import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {
     Dish findDishById(Integer id) {
          DBConnection dbConnection = new DBConnection();
          Connection connection = dbConnection.getConnection();
          try {
               PreparedStatement preparedStatement = connection.prepareStatement(
                       """
                               select dish.id as dish_id, dish.name as dish_name, dish_type, dish.price as dish_price
                               from dish
                               where dish.id = ?;
                               """);
               preparedStatement.setInt(1, id);
               ResultSet resultSet = preparedStatement.executeQuery();
               if (resultSet.next()) {
                    Dish dish = new Dish();
                    dish.setId(resultSet.getInt("dish_id"));
                    dish.setName(resultSet.getString("dish_name"));
                    dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                    dish.setPrice(resultSet.getObject("dish_price") == null
                            ? null : resultSet.getDouble("dish_price"));
                    dish.setIngredients(findIngredientByDishId(id));
                    return dish;
               }
               dbConnection.closeConnection(connection);
               throw new RuntimeException("Dish not found " + id);
          } catch (SQLException e) {
               throw new RuntimeException(e);
          }
     }

     Dish saveDish(Dish toSave) {
          String upsertDishSql = """
                    INSERT INTO dish (id, price, name, dish_type)
                    VALUES (?, ?, ?, ?::dish_type)
                    ON CONFLICT (id) DO UPDATE
                    SET name = EXCLUDED.name,
                        dish_type = EXCLUDED.dish_type
                    RETURNING id
                """;

          try (Connection conn = new DBConnection().getConnection()) {
               conn.setAutoCommit(false);
               Integer dishId;
               try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                    if (toSave.getId() != null) {
                         ps.setInt(1, toSave.getId());
                    } else {
                         ps.setInt(1, getNextSerialValue(conn, "dish", "id"));
                    }
                    if (toSave.getPrice() != null) {
                         ps.setDouble(2, toSave.getPrice());
                    } else {
                         ps.setNull(2, Types.DOUBLE);
                    }
                    ps.setString(3, toSave.getName());
                    ps.setString(4, toSave.getDishType().name());
                    try (ResultSet rs = ps.executeQuery()) {
                         rs.next();
                         dishId = rs.getInt(1);
                    }
               }

               List<Ingredient> newIngredients = toSave.getIngredients();
               detachIngredients(conn, dishId, newIngredients);
               attachIngredients(conn, dishId, newIngredients);

               conn.commit();
               return findDishById(dishId);
          } catch (SQLException e) {
               throw new RuntimeException(e);
          }
     }

     public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
          if (newIngredients == null || newIngredients.isEmpty()) {
               return List.of();
          }
          List<Ingredient> savedIngredients = new ArrayList<>();
          DBConnection dbConnection = new DBConnection();
          Connection conn = dbConnection.getConnection();
          try {
               conn.setAutoCommit(false);
               String insertSql = """
                        INSERT INTO ingredient (id, name, category, price, required_quantity)
                        VALUES (?, ?, ?::ingredient_category, ?, ?)
                        RETURNING id
                    """;
               try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (Ingredient ingredient : newIngredients) {
                         if (ingredient.getId() != null) {
                              ps.setInt(1, ingredient.getId());
                         } else {
                              ps.setInt(1, getNextSerialValue(conn, "ingredient", "id"));
                         }
                         ps.setString(2, ingredient.getName());
                         ps.setString(3, ingredient.getCategory().name());
                         ps.setDouble(4, ingredient.getPrice());
                         if (ingredient.getQuantity() != null) {
                              ps.setDouble(5, ingredient.getQuantity());
                         } else {
                              ps.setNull(5, Types.DOUBLE);
                         }

                         try (ResultSet rs = ps.executeQuery()) {
                              rs.next();
                              int generatedId = rs.getInt(1);
                              ingredient.setId(generatedId);
                              savedIngredients.add(ingredient);
                         }
                    }
                    conn.commit();
                    return savedIngredients;
               } catch (SQLException e) {
                    conn.rollback();
                    throw new RuntimeException(e);
               }
          } catch (SQLException e) {
               throw new RuntimeException(e);
          } finally {
               dbConnection.closeConnection(conn);
          }
     }


     private void detachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredients)
             throws SQLException {
          if (ingredients == null || ingredients.isEmpty()) {
               try (PreparedStatement ps = conn.prepareStatement(
                       "UPDATE ingredient SET id_dish = NULL WHERE id_dish = ?")) {
                    ps.setInt(1, dishId);
                    ps.executeUpdate();
               }
               return;
          }

          String baseSql = """
                      UPDATE ingredient
                      SET id_dish = NULL
                      WHERE id_dish = ? AND id NOT IN (%s)
                  """;

          String inClause = ingredients.stream()
                  .map(i -> "?")
                  .collect(Collectors.joining(","));

          String sql = String.format(baseSql, inClause);

          try (PreparedStatement ps = conn.prepareStatement(sql)) {
               ps.setInt(1, dishId);
               int index = 2;
               for (Ingredient ingredient : ingredients) {
                    ps.setInt(index++, ingredient.getId());
               }
               ps.executeUpdate();
          }
     }

     private void attachIngredients(Connection conn, Integer dishId, List<Ingredient> ingredients)
             throws SQLException {

          if (ingredients == null || ingredients.isEmpty()) {
               return;
          }

          String attachSql = """
                    UPDATE ingredient
                    SET id_dish = ?
                    WHERE id = ?
                """;

          try (PreparedStatement ps = conn.prepareStatement(attachSql)) {
               for (Ingredient ingredient : ingredients) {
                    ps.setInt(1, dishId);
                    ps.setInt(2, ingredient.getId());
                    ps.addBatch(); // Can be substitute ps.executeUpdate() but bad performance
               }
               ps.executeBatch();
          }
     }

     private List<Ingredient> findIngredientByDishId(Integer idDish) {
          DBConnection dbConnection = new DBConnection();
          Connection connection = dbConnection.getConnection();
          List<Ingredient> ingredients = new ArrayList<>();
          try {
               PreparedStatement preparedStatement = connection.prepareStatement(
                       """
                               select ingredient.id, ingredient.name, ingredient.price, ingredient.category, ingredient.required_quantity
                               from ingredient where id_dish = ?;
                               """);
               preparedStatement.setInt(1, idDish);
               ResultSet resultSet = preparedStatement.executeQuery();
               while (resultSet.next()) {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setId(resultSet.getInt("id"));
                    ingredient.setName(resultSet.getString("name"));
                    ingredient.setPrice(resultSet.getDouble("price"));
                    ingredient.setCategory(CategoryEnum.valueOf(resultSet.getString("category")));
                    Object requiredQuantity = resultSet.getObject("required_quantity");
                    ingredient.setQuantity(requiredQuantity == null ? null : resultSet.getDouble("required_quantity"));
                    ingredients.add(ingredient);
               }
               dbConnection.closeConnection(connection);
               return ingredients;
          } catch (SQLException e) {
               throw new RuntimeException(e);
          }
     }


     private String getSerialSequenceName(Connection conn, String tableName, String columnName)
             throws SQLException {

          String sql = "SELECT pg_get_serial_sequence(?, ?)";

          try (PreparedStatement ps = conn.prepareStatement(sql)) {
               ps.setString(1, tableName);
               ps.setString(2, columnName);

               try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                         return rs.getString(1);
                    }
               }
          }
          return null;
     }

     private int getNextSerialValue(Connection conn, String tableName, String columnName)
             throws SQLException {

          String sequenceName = getSerialSequenceName(conn, tableName, columnName);
          if (sequenceName == null) {
               throw new IllegalArgumentException(
                       "Any sequence found for " + tableName + "." + columnName
               );
          }
          updateSequenceNextValue(conn, tableName, columnName, sequenceName);

          String nextValSql = "SELECT nextval(?)";

          try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
               ps.setString(1, sequenceName);
               try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
               }
          }
     }

     private void updateSequenceNextValue(Connection conn, String tableName, String columnName, String sequenceName) throws SQLException {
          String setValSql = String.format(
                  "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s))",
                  sequenceName, columnName, tableName
          );

          try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
               ps.executeQuery();
          }
     }


     public List<DishIngredient> findIngredientsByDishId(Integer dishId) {

          List<DishIngredient> result = new ArrayList<>();

          String sql = """
                      SELECT i.id, i.name, i.price, i.category,
                             di.quantity, di.unit
                      FROM DishIngredient di
                      JOIN Ingredient i ON di.ingredient_id = i.id
                      WHERE di.dish_id = ?
                  """;

          try (PreparedStatement ps = connection.prepareStatement(sql)) {
               ps.setInt(1, dishId);
               ResultSet rs = ps.executeQuery();

               while (rs.next()) {
                    Ingredient ingredient = new Ingredient(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getDouble("price"),
                            CategoryEnum.valueOf(rs.getString("category"))
                    );

                    DishIngredient di = new DishIngredient(
                            null,
                            ingredient,
                            rs.getDouble("quantity"),
                            rs.getString("unit")
                    );

                    result.add(di);
               }
          }
          return result;
     }

     public Ingredient saveIngredient(Ingredient toSave) {

          String ingredientSql = """
        INSERT INTO Ingredient(name, price, category)
        VALUES (?, ?, ?)
        ON CONFLICT (name) DO NOTHING
        RETURNING id
    """;

          try (PreparedStatement ps = connection.prepareStatement(ingredientSql)) {
               ps.setString(1, toSave.getName());
               ps.setDouble(2, toSave.getPrice());
               ps.setString(3, toSave.getCategory().name());

               ResultSet rs = ps.executeQuery();
               if (rs.next()) {
                    toSave.setId(rs.getInt("id"));
               }
          }

          saveStockMovements(toSave);

          return toSave;
     }

     private void saveStockMovements(Ingredient ingredient) {

          String sql = """
        INSERT INTO StockMovement(id, ingredient_id, quantity, unit, movement_date)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (id) DO NOTHING
    """;

          for (StockMovement sm : ingredient.getStockMovementList()) {
               try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setObject(1, sm.getId());
                    ps.setInt(2, ingredient.getId());
                    ps.setDouble(3, sm.getQuantity());
                    ps.setString(4, sm.getUnit());
                    ps.setTimestamp(5, Timestamp.from(sm.getMovementDate()));
                    ps.executeUpdate();
               }
          }
     }
}


