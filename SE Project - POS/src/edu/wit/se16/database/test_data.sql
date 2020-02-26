INSERT INTO `employees` (firstname, lastname, role, password_hash, password_salt)
	VALUES ("Andy", "Ngo", "Server", 
			"+HY0oqqJUw/r/qTG9B4qYe3FArLQq4ygRrXqk4qtA==",  /* Password: YsRflh */
			"+ygXvL/p65Zf7/yyCpiGRZnGK8tklaCj1PNWURPLwp5jxqxT/MOSR/685HFGT/qByXJ4zIz808XBZHlsA=="); 
            
INSERT INTO `tables` (table_number, table_descriptor) VALUES ("6", "table_rrect_2x2");
INSERT INTO `table_status_history` (employee_id, table_id, status) VALUES(NULL, (SELECT ID FROM `tables` LIMIT 1), "Open");

INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(8, 3), POINT(7, 7), 0, (SELECT ID FROM `tables` LIMIT 1));
INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(23, 5), POINT(1, 15), 0, NULL);
INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(23, 19), POINT(5, 1), 0, NULL);
INSERT INTO `restaurant_layout` (position, bounds, rotation, is_table) VALUES(POINT(27, 8), POINT(5, 8), 0, NULL);