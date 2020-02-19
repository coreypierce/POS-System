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
	`password_hash` VARCHAR(45) NOT NULL,
	`password_salt` VARCHAR(45) NOT NULL,
	`active` TINYINT NULL DEFAULT 1,
	`deleted` TINYINT NULL DEFAULT 0,
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

/* ********************************* ***** ********************************* */
/* ********************************* Table ********************************* */

CREATE TABLE `tables` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`table_descriptor` VARCHAR(45) NOT NULL,
	`table_number` INT UNSIGNED NOT NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC, `table_number` ASC));


/* ********************************* ***** ********************************* */
/* ********************************* Order ********************************* */

CREATE TABLE `orders` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`server_id` INT UNSIGNED NOT NULL,
	`table_id` INT UNSIGNED NOT NULL,
	`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
PRIMARY KEY (`id`),
UNIQUE 
	INDEX `id_UNIQUE` (`id` ASC),
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

/* ********************************* ***************** ********************************* */
/* ********************************* Restaurant Layout ********************************* */

CREATE TABLE `restaurant_layout` (
	`id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
	`is_table` INT UNSIGNED NULL,
	`position` POINT NOT NULL,
	`bounds` POINT NOT NULL,
PRIMARY KEY (`id`),
UNIQUE INDEX `id_UNIQUE` (`id` ASC),
UNIQUE INDEX `is_table_UNIQUE` (`is_table` ASC),

CONSTRAINT `restaurant_layout_FOREIGN_KEY_table`
FOREIGN KEY (`is_table`) REFERENCES `pos`.`tables` (`id`)
	ON DELETE CASCADE
	ON UPDATE CASCADE
);
