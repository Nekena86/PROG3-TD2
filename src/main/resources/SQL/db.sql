CREATE USER mini_dish_db WITH PASSWORD '123456';

CREATE DATABASE mini_dish_db_manager 
   OWNER mini_dish_db
   ENCODING 'UTF8';

GRANT ALL PRIVILEGES ON DATABASE mini_dish_db TO mini_dish_db_manager;

