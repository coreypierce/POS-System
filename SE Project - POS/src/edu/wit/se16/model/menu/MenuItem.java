package edu.wit.se16.model.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
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
	
	// =========================================== JSON =========================================== \\
	
	public JsonNode toJSON() {
		return JsonBuilder.create()
			.append("id", super.id)
			.append("name", name)
			.append("price", price)
			.append("category_id", category_id)
		.build();
	}
}
