INSERT INTO `employees` (firstname, lastname, role, password_hash, password_salt)
	VALUES ("Andy", "Ngo", "Server", 
			"olmYQRbHSTyLw/aEA6fRMtjrB/13Xg9z48z+5yl7v9t9J5+eE3FkUhqKYicZW5RSkunLfVy1bgngKALwbmhEHw==",  /* Password: gQcytP */
			"CQHXBEYiP/beKahnzD//Ys/I4YVkeDsTobBoGcZyJ5/jYUPzfiwV28h+0pAKSJaZw+p/c+lp2CyWpmDDEVUohg=="); 
            
INSERT INTO `employees` (firstname, lastname, role, password_hash, password_salt)
	VALUES ("Corey", "Piercec", "Manager", 
			"olmYQRbHSTyLw/aEA6fRMtjrB/13Xg9z48z+5yl7v9t9J5+eE3FkUhqKYicZW5RSkunLfVy1bgngKALwbmhEHw==",  /* Password: gQcytP */
			"CQHXBEYiP/beKahnzD//Ys/I4YVkeDsTobBoGcZyJ5/jYUPzfiwV28h+0pAKSJaZw+p/c+lp2CyWpmDDEVUohg=="); 
            
INSERT INTO `tables` (table_number, table_descriptor) VALUES ("3", "table_rrect_2x2");
INSERT INTO `tables` (table_number, table_descriptor) VALUES ("6", "table_rrect_2x2");
INSERT INTO `table_status_history` (employee_id, table_id, status) VALUES(NULL, (SELECT ID FROM `tables` WHERE table_number = '3' LIMIT 1), "Open");
INSERT INTO `table_status_history` (employee_id, table_id, status) VALUES(NULL, (SELECT ID FROM `tables` WHERE table_number = '6' LIMIT 1), "Open");

INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(27, 3), POINT(7, 7), 0, 
	(SELECT ID FROM `tables` WHERE table_number = '3' LIMIT 1));
INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(8, 3), POINT(7, 7), 0, 
	(SELECT ID FROM `tables` WHERE table_number = '6' LIMIT 1));
    
INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(23, 5), POINT(1, 15), 0, NULL);
INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(23, 19), POINT(5, 1), 0, NULL);
INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(24, 11), POINT(5, 8), 0, NULL);

INSERT INTO `shifts` (shift_type, manager_id) VALUES('Noon', (SELECT ID FROM `employees` WHERE role = 'Manager' LIMIT 1));
INSERT INTO `sections` (shift_id, section_number, assignee_id) VALUES(
	(SELECT ID FROM `shifts` LIMIT 1), 0, (SELECT ID FROM `employees` WHERE role = 'Server' LIMIT 1));
    
INSERT INTO `section_tables` (section_id, table_id) VALUES(
	(SELECT ID FROM `sections` LIMIT 1), 
	(SELECT ID FROM `tables` WHERE table_number = '6' LIMIT 1));
    
    
INSERT INTO `menu_categories` (name) VALUES('Food');
INSERT INTO `menu_categories` (name) VALUES('Drinks');
    
INSERT INTO `menu_items` (category_id, name, price) VALUES(1, 'Food A', 3.95);
INSERT INTO `menu_items` (category_id, name, price) VALUES(1, 'Food B', 5.50);
INSERT INTO `menu_items` (category_id, name, price) VALUES(2, 'Vodka', 7.99);