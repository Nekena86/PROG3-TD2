CREATE TYPE ingredient_enum AS ENUM (
    'VEGETABLE',
    'ANIMAL',
    'MARINE',
    'DAIRY',
    'OTHER'
);

CREATE TYPE dish_type AS ENUM (
    'START',
    'MAIN',
    'DESSERT'
);

CREATE TABLE dish (
                      id INT PRIMARY KEY,
                      name VARCHAR(255),
                      dish_type dish_type
);

CREATE TABLE ingredient (
                            id INT PRIMARY KEY,
                            name VARCHAR(255),
                            price NUMERIC,
                            category ingredient_enum,
                            id_dish INT REFERENCES dish(id)
);

