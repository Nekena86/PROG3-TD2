CREATE TABLE Dish (
                      id SERIAL PRIMARY KEY,
                      name VARCHAR(100) NOT NULL,
                      dish_type VARCHAR(20) NOT NULL,
                      selling_price DOUBLE
);

CREATE TABLE Ingredient (
                            id SERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            price DOUBLE  NOT NULL,
                            category VARCHAR(30) NOT NULL
);

CREATE TABLE DishIngredient (
                                dish_id INTEGER NOT NULL,
                                ingredient_id INTEGER NOT NULL,
                                quantity DOUBLE  NOT NULL,
                                unit VARCHAR(10) NOT NULL,

                                PRIMARY KEY (dish_id, ingredient_id),

                                CONSTRAINT fk_dish
                                    FOREIGN KEY (dish_id) REFERENCES Dish(id),
                                CONSTRAINT fk_ingredient
                                    FOREIGN KEY (ingredient_id) REFERENCES Ingredient(id)
);

CREATE TABLE StockMovement (
                               id SERIAL PRIMARY KEY,
                               ingredient_id INTEGER NOT NULL,
                               quantity DOUBLE PRECISION NOT NULL,
                               unit VARCHAR(10) NOT NULL,
                               movement_date TIMESTAMP NOT NULL,

                               CONSTRAINT fk_ingredient
                                   FOREIGN KEY (ingredient_id)
                                       REFERENCES Ingredient(id)
);
