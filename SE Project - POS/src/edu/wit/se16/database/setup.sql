DROP DATABASE IF EXISTS `pos`;

/* Creates the Database, and sets it as the default-schema */
CREATE DATABASE `pos`;
USE `pos`;

/* ********************************* ********* ********************************* */
/* ********************************* Employees ********************************* */

CREATE TABLE `employees` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`firstname` VARCHAR(45) NOT NULL,
	`lastname` VARCHAR(45) NOT NULL,
	`role` ENUM('Server', 'Host', 'Manager') NOT NULL DEFAULT 'Server',
	`password_hash` VARCHAR(88) NOT NULL,
	`password_salt` VARCHAR(88) NOT NULL,
	`active` TINYINT NULL DEFAULT 1,
	`deleted` TINYINT NULL DEFAULT 0,
    `created_on` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC));
  
DELIMITER $$

/* Define PROCEDURE to generate unique 6-digit Employee-IDs */
CREATE FUNCTION generateEmployeeID()
RETURNS INT UNSIGNED
BEGIN
	DECLARE new_id INT UNSIGNED DEFAULT 0;

	WHILE 
		new_id = 0 OR EXISTS(SELECT id FROM employees WHERE id = new_id)
	DO
		SET new_id = FLOOR(RAND() * 899999 + 100000);
	END WHILE;
    
	RETURN new_id;
END$$

/* Before Employee-Insert generate ID */
CREATE TRIGGER `employees_GEN_ID` 
BEFORE INSERT 
	ON `employees` FOR EACH ROW
BEGIN
	SET NEW.id = generateEmployeeID();
END$$

DELIMITER ;

/* ********************************* ************ ********************************* */
/* ********************************* Clock in/out ********************************* */

CREATE TABLE `employee_timestamps` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`employee_id` INT UNSIGNED NOT NULL,
	`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
	INDEX `employee_timestamp_FOREIGN_KEY_employee_idx` (`employee_id` ASC),
    
CONSTRAINT `employee_timestamp_FOREIGN_KEY_employee`
FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);

/* ********************************* ************** ********************************* */
/* ********************************* Session Tokens ********************************* */

CREATE TABLE `session_tokens` (
	`id` CHAR(24) NOT NULL,
	`employee_id` INT UNSIGNED NOT NULL,
	`expiration` TIMESTAMP NOT NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
	INDEX `session_token_FOREIGN_KEY_employee_idx` (`employee_id` ASC),
    
CONSTRAINT `session_token_FOREIGN_KEY_employee`
FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);

/* ********************************* ***** ********************************* */
/* ********************************* Table ********************************* */

CREATE TABLE `tables` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`table_descriptor` VARCHAR(45) NOT NULL,
	`table_number` INT UNSIGNED NOT NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
	INDEX `table_number_INDEX` (`table_number` ASC));
    
CREATE TABLE `table_status_history` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`employee_id` INT UNSIGNED NULL,
	`table_id` INT UNSIGNED NOT NULL,
	`status` ENUM('Open', 'Seated', 'Order_Placed', 'Check_Printed', 'Check_In') NOT NULL,
	`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
	INDEX `table_status_history_FOREIGN_KEY_employee_idx` (`employee_id` ASC),
	INDEX `table_status_history_FOREIGN_KEY_table_idx` (`table_id` ASC),
    
CONSTRAINT `table_status_history_FOREIGN_KEY_employee`
FOREIGN KEY (`employee_id`) REFERENCES `employees` (`id`)
	ON DELETE SET NULL
	ON UPDATE CASCADE,
    
CONSTRAINT `table_status_history_FOREIGN_KEY_table`
FOREIGN KEY (`table_id`) REFERENCES `tables` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);

/* ********************************* **** ********************************* */
/* ********************************* Menu ********************************* */

CREATE TABLE `menu_categories` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(45) NOT NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
UNIQUE INDEX `name_UNIQUE` (`name` ASC));

CREATE TABLE `menu_items` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(45) NOT NULL,
	`price` DECIMAL(5, 2) UNSIGNED NOT NULL,
	`category_id` INT UNSIGNED NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
UNIQUE INDEX `name_UNIQUE` (`name` ASC),
	INDEX `menu_item_FOREIGN_KEY_category_idx` (`category_id` ASC),
    
CONSTRAINT `menu_item_FOREIGN_KEY_category`
FOREIGN KEY (`category_id`) REFERENCES `menu_categories` (`id`)
	ON DELETE SET NULL
	ON UPDATE CASCADE
);

/* ********************************* ***** ********************************* */
/* ********************************* Order ********************************* */

CREATE TABLE `orders` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`server_id` INT UNSIGNED NOT NULL,
	`table_id` INT UNSIGNED NOT NULL,
	`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
	INDEX `orders_FOREIGN_KEY_server_idx` (`server_id` ASC),
	INDEX `orders_FOREIGN_KEY_table_idx` (`table_id` ASC),
    
CONSTRAINT `orders_FOREIGN_KEY_server`
FOREIGN KEY (`server_id`) REFERENCES `employees` (`id`)
	ON DELETE RESTRICT
	ON UPDATE CASCADE,
    
CONSTRAINT `orders_FOREIGN_KEY_table`
FOREIGN KEY (`table_id`) REFERENCES `tables` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);

CREATE TABLE `order_items` (
	`order_id` INT UNSIGNED NOT NULL,
	`item_id` INT UNSIGNED NOT NULL,
	`quantity` INT UNSIGNED NOT NULL DEFAULT 1,
PRIMARY KEY (`order_id`, `item_id`),
INDEX `order_item_FOREIN_KEY_item_idx` (`item_id` ASC),

CONSTRAINT `order_item_FOREIN_KEY_order`
FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE,
    
CONSTRAINT `order_item_FOREIN_KEY_item`
FOREIGN KEY (`item_id`) REFERENCES `menu_items` (`id`)
	ON DELETE RESTRICT
	ON UPDATE CASCADE
);

/* ********************************* ***** ********************************* */
/* ********************************* Shift ********************************* */

CREATE TABLE `shifts` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`date` DATETIME NOT NULL DEFAULT NOW(),
	`shift_type` ENUM('Morning', 'Noon', 'Evening', 'Night') NOT NULL,
	`manager_id` INT UNSIGNED NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
	INDEX `shft_FOREIGN_KEY_manager_idx` (`manager_id` ASC),
    
CONSTRAINT `shift_FOREIGN_KEY_manager`
FOREIGN KEY (`manager_id`) REFERENCES `employees` (`id`)
	ON DELETE SET NULL
	ON UPDATE CASCADE
);

/* ********************************* ******** ********************************* */
/* ********************************* Specials ********************************* */

CREATE TABLE `specials` (
	`shift_id` INT UNSIGNED NOT NULL,
	`item_id` INT UNSIGNED NOT NULL,
	`price` DECIMAL(5, 2) UNSIGNED NOT NULL,
PRIMARY KEY (`shift_id`, `item_id`),
INDEX `special_FOREIGN_KEY_item_idx` (`item_id` ASC),

CONSTRAINT `special_FOREIGN_KEY_shift`
FOREIGN KEY (`shift_id`) REFERENCES `shifts` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE,
    
CONSTRAINT `special_FOREIGN_KEY_item`
FOREIGN KEY (`item_id`) REFERENCES `menu_items` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);

/* ********************************* ******** ********************************* */
/* ********************************* Sections ********************************* */

CREATE TABLE `sections` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`section_number` INT UNSIGNED NOT NULL,
	`assignee_id` INT UNSIGNED NULL,
	`shift_id` INT UNSIGNED NOT NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
	INDEX `section_number` (`section_number` ASC),
	INDEX `sections_FOREIGN_KEY_assignee_idx` (`assignee_id` ASC),
	INDEX `sections_FOREIGN_KEY_shift_idx` (`shift_id` ASC),
    
CONSTRAINT `sections_FOREIGN_KEY_assignee`
FOREIGN KEY (`assignee_id`) REFERENCES `employees` (`id`)
	ON DELETE SET NULL
	ON UPDATE CASCADE,
    
CONSTRAINT `sections_FOREIGN_KEY_shift`
FOREIGN KEY (`shift_id`) REFERENCES `shifts` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);

CREATE TABLE `section_tables` (
	`section_id` INT UNSIGNED NOT NULL,
	`table_id` INT UNSIGNED NOT NULL,
PRIMARY KEY (`section_id`, `table_id`),
INDEX `section_table_FOREIGN_KEY_table_idx` (`table_id` ASC),

CONSTRAINT `section_table_FOREIGN_KEY_section`
FOREIGN KEY (`section_id`) REFERENCES `specials` (`shift_id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE,
    
CONSTRAINT `section_table_FOREIGN_KEY_table`
FOREIGN KEY (`table_id`) REFERENCES `tables` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);


/* ********************************* ***************** ********************************* */
/* ********************************* Restaurant Layout ********************************* */

CREATE TABLE `restaurant_layout` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`is_table` INT UNSIGNED NULL,
	`position` POINT NOT NULL,
	`bounds` POINT NOT NULL,
    `rotation` INT NOT NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
UNIQUE INDEX `is_table_UNIQUE` (`is_table` ASC),

CONSTRAINT `restaurant_layout_FOREIGN_KEY_table`
FOREIGN KEY (`is_table`) REFERENCES `pos`.`tables` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);
