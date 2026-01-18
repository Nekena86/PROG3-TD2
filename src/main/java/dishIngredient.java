public class dishIngredient {
    private Dish dish;
    private Ingredient ingredient;
    private Double quantity;
    private String unit;

    public dishIngredient(Dish dish, Ingredient ingredient, Double quantity, String unit) {
        this.dish = dish;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = unit;
    }

    public Double getCost() {
        return ingredient.getPrice() * quantity;
    }
}
