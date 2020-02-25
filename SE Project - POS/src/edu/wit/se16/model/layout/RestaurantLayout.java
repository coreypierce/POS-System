package edu.wit.se16.model.layout;

import java.awt.Dimension;
import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.model.Table;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RestaurantLayout {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final String QUERY_ITEM_SQL = 
		"SELECT " +
			"is_table, " +
			"ST_X(position) as pos_x, ST_Y(position) as pos_y, " +
			"ST_X(bounds) as width, ST_Y(bounds) as height " +
		"FROM restaurant_layout";

	private static final PreparedStatement QUERY_LAYOUT = Database.prep(QUERY_ITEM_SQL);
	
	private static final PreparedStatement QUERY_ITEM = Database.prep(QUERY_ITEM_SQL + " WHERE ID = ?");
	private static final PreparedStatement INSERT_ITEM = Database.prep(
			"INSERT INTO restaurant_layout (location, bounds, is_table) VALUES(POINT(?, ?), POINT(?, ?), ?)");
	
// ============================================ Instance ============================================ \\
	
	/** Static single-instance to use through program */
	private static RestaurantLayout INSTANCE;
	
	public static RestaurantLayout getLayout() {
		// if no instance is loaded
		if(INSTANCE == null) {
			reloadFromDatabase();
		}
		
		return INSTANCE;
	}
	
	/** Reload static INSTANCE from Database */
	private static void reloadFromDatabase() {
		LOG.trace("Loading Restaurant-Layout from database...");

		// WARN: any un-committed changes will be lost
		INSTANCE = new RestaurantLayout();
		INSTANCE.queryItems();
		
		LOG.trace("Restaurant-Layout successfuly loaded!");
	}

// ============================================ ============= ============================================ \\
// ============================================ End of Static ============================================ \\
// ============================================ ============= ============================================ \\
	
	private Collection<Item> items;
	private int width, height;
	
	private RestaurantLayout() {
		this.items = new ArrayList<>();
		
		// don't let width/height == 0
		this.width = 1;
		this.height = 1;
	}
	
	private void queryItems() {
		this.items = new ArrayList<>();
		
		Database.query(result -> {
			Item item = Item.from(result);
			this.items.add(item);
			
			// check/update layout bounds
			int limit_x = item.location.x + item.bounds.width;
			int limit_y = item.location.y + item.bounds.height;
			
			if(limit_x > width) this.width = limit_x;
			if(limit_y > height) this.height = limit_y;
		}, QUERY_LAYOUT);
	}
	
	public JsonNode toJSON() {
		JsonBuilder builder = JsonBuilder.create()
			.append("width", width)
			.append("height", height);

		builder.newArray("items");
		for(Item item : items) {
			builder.append(item.toJSON());
		}
		
		builder.end();
		return builder.build();
	}
	
	/**
	 * 	Represents a wall or table to be draw in restaurant view. <br />
	 * 	{@code table} will be {@code null} if Item is a wall
	 */
	public static class Item extends DatabaseObject {
		private Point location;
		private Dimension bounds;
		
		private Table table;
		
		public Item(int id) {
			super(id);
		}
		
		private Item() {}
		
		public static Item from(ResultSet result) throws SQLException {
			Item item = new Item();
			item.loadFromRow(result);
			return item;
		}

		// =========================================== DB - Object =========================================== \\
		
		public void loadFromRow(ResultSet result) throws SQLException {
			this.location = new Point(result.getInt("pos_x"), result.getInt("pos_y"));
			this.bounds = new Dimension(result.getInt("width"), result.getInt("height"));
			
			// check if this is a wall or table
			Integer table_id = result.getObject("is_table", Integer.class);
			// is this a valid table-id
			if(table_id != null && table_id > 0) {
				// if table, then load/query table-object
				this.table = new Table(table_id);
			}
		}
		
		protected boolean query() {
			return Database.query(this::loadFromRow, QUERY_ITEM, id);
		}

		protected boolean insert() {
			if(id != 0) { 
				LOG.error("Layout-Item has already been created!");
				return false; 
			}
			
			LOG.trace("Inserting new Layout-Item...");
			// call update request
			if(Database.update(INSERT_ITEM, 
					location.x, location.y,
					bounds.width, bounds.height, 
					table != null ? table.getId() : null)
			) {
				try {
					// grab last generated id
					ResultSet prev_key = INSERT_ITEM.getGeneratedKeys();
					if(!prev_key.next()) throw new SQLException("Failed to get item-id; no rows returned!");
					this.id = prev_key.getInt(1);

					return true;
					
				} catch (SQLException e) {
					LOG.error("An error occured while getting item-id", e);
					return false;
				}
			} else {
				LOG.warn("Layout-Item INSERT failed!");
				return false;
			}
		}
		
// =========================================== JSON =========================================== \\
		
		public JsonNode toJSON() {
			JsonBuilder builder = JsonBuilder.create()
				.newObject("position")
					.append("x", location.x)
					.append("y", location.y)
				.end()
				.newObject("bounds")
					.append("width", bounds.width)
					.append("height", bounds.height)
				.end();
			
			if(table != null) {
				builder.append("table", table.toJSON());
			} else {
				builder.appendNull("table");
			}
			
			return builder.build();
		}
	}
}
