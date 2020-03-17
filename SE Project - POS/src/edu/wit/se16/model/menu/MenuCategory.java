package edu.wit.se16.model.menu;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.system.logging.LoggingUtil;

public class MenuCategory extends DatabaseObject {
private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM menu_categories WHERE id = ? OR name = ?");
	private static final PreparedStatement INSERT = Database.prep("INSERT INTO menu_categories (name) VALUES (?)");
	
	private String name;
	
	public MenuCategory(int id) {
		super(id);
	}
	
	public MenuCategory(String name) {
		this.name = name;
		
		if(!query()) {	// attempt to get back the id (and other fields)
			insert(); 	// if it doesn't exists, then insert it
		}
	}
	
	protected boolean query() {
		return Database.query(result -> {
			this.id = result.getInt("id");
			this.name = result.getString("name");
		}, QUERY, id, name);
	}
	
	protected boolean insert() {
		if(id != 0) { 
			LOG.error("Menu-Category has already been created!");
			return false; 
		}
		
		LOG.trace("Inserting new Menu-Category({})...", name);
		// call update request
		if(Database.update(INSERT, name)) {
			try {
				// grab last generated id
				ResultSet prev_key = INSERT.getGeneratedKeys();
				if(!prev_key.next()) throw new SQLException("Failed to get menu_category-id; no rows returned!");
				this.id = prev_key.getInt(1);

				return true;
				
			} catch (SQLException e) {
				LOG.error("An error occured while getting menu_category-id", e);
				return false;
			}
		} else {
			LOG.warn("Menu-Category INSERT failed!");
			return false;
		}
	}
}
