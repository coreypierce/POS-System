package edu.wit.se16.model.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class MenuItem extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM menu_items WHERE ID = ?");
	private static final PreparedStatement INSERT = Database.prep(
			"INSERT INTO menu_items (name, price, category_id) VALUES (?, ?, ?)");
	
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
	
	// =========================================== JSON =========================================== \\
	
	public JsonNode toJSON() {
		return JsonBuilder.create()
			.append("id", super.id)
			.append("name", name)
			.append("category_id", category_id)
		.build();
	}
}
