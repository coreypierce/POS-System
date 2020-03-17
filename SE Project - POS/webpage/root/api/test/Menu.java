package root.api.test;

import java.sql.PreparedStatement;

import edu.wit.se16.database.Database;

public class Menu {
	private static final PreparedStatement addMenuItem = Database.prep("INSERT INTO MENU_ITEMS (name,price,category_id)"
			+ "VALUES('',?,?,'')");
	private static final PreparedStatement deleteMenuItem = Database.prep("DELETE FROM MENU_ITEMS WHERE id = ?");
	
}