package root.api.test;

import java.sql.PreparedStatement;

import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.system.logging.LoggingUtil;

public class Menu {
	private static final PreparedStatement addMenuItem = Database.prep("INSERT INTO MENU_ITEMS (name,price)"
			+ "VALUES(?,?)");
	private static final PreparedStatement deleteMenuItem = Database.prep("DELETE FROM MENU_ITEMS WHERE id = ?");
	private static final Logger LOG = LoggingUtil.getLogger();
	public static void addItemToMenu(String item, double price) {
		Database.update(addMenuItem, item, price);
	}
	public static void removeItemFromMenu(int itemID) {
		Database.update(deleteMenuItem, itemID);
	}
}