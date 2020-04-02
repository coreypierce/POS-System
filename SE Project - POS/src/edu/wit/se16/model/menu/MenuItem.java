package edu.wit.se16.model.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.model.Shift;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class MenuItem extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY_ALL_IDS = Database.prep("SELECT id FROM menu_items");
	
	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM menu_items WHERE ID = ?");
	private static final PreparedStatement INSERT = Database.prep(
			"INSERT INTO menu_items (name, price, category_id) VALUES (?, ?, ?)");

	private static final PreparedStatement UPDATE = Database.prep(
			"UPDATE menu_items SET category_id = ?, name = ?, price = ? WHERE id = ?");
	
	private static final PreparedStatement DELETE = Database.prep("DELETE FROM menu_items WHERE id = ?");

	private static final PreparedStatement QUERY_SPECIAL = Database.prep(
			"SELECT price FROM specials  WHERE shift_id = ? AND item_id = ?");
	
	private static final PreparedStatement INSERT_SPECIAL = Database.prep(
			"INSERT INTO specials (shift_id, item_id, price) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE price = ?");
	
	private static final PreparedStatement DELETE_SPECIAL = Database.prep("DELETE FROM specials WHERE shift_id = ? AND item_id = ?");
	
	
	
	private String name;
	private double price;
	
	private int category_id;
	
	public MenuItem(int id) {
		super(id);
	}
	
	public MenuItem(String name, double price, MenuCategory category) {
		this.name = name;
		this.price = price;
		
		this.category_id = category.getId();
		

		insert(); // insert into database
		query(); // re-query fields
	}

	// =========================================== DB - Object =========================================== \\
	
	protected boolean query() {
		return Database.query(result -> {
			this.name = result.getString("name");
			this.price = result.getDouble("price");
			
			this.category_id = result.getInt("category_id");
		}, QUERY, id);
	}
	
	protected boolean insert() {
		if(id != 0) { 
			LOG.error("Menu-Item has already been created!");
			return false; 
		}
		
		LOG.trace("Inserting new Menu-Item({}, ${}) into Category #{}...", name, price, category_id);
		// call update request
		if(Database.update(INSERT, name, price, category_id)) {
			try {
				// grab last generated id
				ResultSet prev_key = INSERT.getGeneratedKeys();
				if(!prev_key.next()) throw new SQLException("Failed to get menu_item-id; no rows returned!");
				this.id = prev_key.getInt(1);

				return true;
				
			} catch (SQLException e) {
				LOG.error("An error occured while getting menu_item-id", e);
				return false;
			}
		} else {
			LOG.warn("Menu-Item INSERT failed!");
			return false;
		}
	}
	
	public static MenuItem[] getAllItems() {
		ArrayList<MenuItem> items = new ArrayList<>();
		
		// errr... not the best way to do this, LOT of sql requests...
		Database.query(result -> {
			items.add(new MenuItem(result.getInt("id")));
		}, QUERY_ALL_IDS);
		
		return items.toArray(new MenuItem[0]);
	}

	// =========================================== Update Item =========================================== \\
	
	public void update(int category_id, String name, double price) {
		if(super.id <= 0) {
			LOG.warn("Attempted to update a Menu-Item that doesn't exists!");
			insert();
			
		} else {
			LOG.trace("Updating Menu-Item #{} to ({}, {}, ${})!", super.id, category_id, name, price);
			Database.update(UPDATE, category_id, name, price, super.id);
		}
		
		LOG.trace("Update complete! Re-querying Menu-Item #{}...", super.id);
		query();
	}
	
	// =========================================== Delete Item =========================================== \\
	
	public void delete() {
		LOG.warn("Deleting Menu-Item #{}; reports will be missing data for \"{}\"!", super.id, name);
		Database.update(DELETE, super.id);
	}
	
	// =========================================== Special Item =========================================== \\
	
	public void markSpecial(Shift shift, double price) {
		LOG.trace("Assigning special price of ${} to Item #{} durring Shift #{}", price, super.id, shift.getId());
		Database.update(INSERT_SPECIAL, shift.getId(), super.id, price, price);
	}
	
	public void removeSpecial(Shift shift) {
		LOG.trace("Remove Item #{} from Shift #{} specials", super.id, shift.getId());
		Database.update(DELETE_SPECIAL, shift.getId(), super.id);
	}
	
	public static Double lookupSpecialPrice(Shift shift, int item_id) {
		if(shift == null) return null;
		
		// no such thing as a AtomicDouble, so store the double's bits in a long
		AtomicLong price_bits = new AtomicLong(-1);
		
		Database.query(
			results -> price_bits.set(Double.doubleToLongBits(results.getDouble("price"))), 
			QUERY_SPECIAL, shift.getId(), item_id);
		
		// return null if there's no special price
		return price_bits.get() < 0 ? null : new Double(Double.longBitsToDouble(price_bits.get()));
	}
	
	// =========================================== JSON =========================================== \\
	
	public JsonNode toJSON(Shift shift) {
		Double special_price = MenuItem.lookupSpecialPrice(shift, super.id);
		
		JsonBuilder builder = JsonBuilder.create()
			.append("id", super.id)
			.append("name", name)
			.append("category_id", category_id);
		
		if(special_price == null) {
			builder.append("price", price);
			
		} else {
			builder.append("price", special_price);
			builder.append("default_price", price);
		}
		
		return builder.build();
	}
	
	public String getName() { return name; }
	public double getPrice() { return price; }
}
