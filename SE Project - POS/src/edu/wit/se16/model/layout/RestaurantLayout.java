package edu.wit.se16.model.layout;

import java.awt.Dimension;
import java.awt.Point;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
			"id, is_table, rotation, " +
			"ST_X(position) as pos_x, ST_Y(position) as pos_y, " +
			"ST_X(bounds) as width, ST_Y(bounds) as height " +
		"FROM restaurant_layout";

	private static final PreparedStatement QUERY_LAYOUT = Database.prep(QUERY_ITEM_SQL);
	
	private static final PreparedStatement QUERY_ITEM = Database.prep(QUERY_ITEM_SQL + " WHERE ID = ?");
	private static final PreparedStatement INSERT_ITEM = Database.prep(
			"INSERT INTO restaurant_layout (rotation, position, bounds, is_table) VALUES(?, POINT(?, ?), POINT(?, ?), ?)");
	
	private static final PreparedStatement UPDATE_ITEM = Database.prep(
			"UPDATE restaurant_layout SET rotation = ?, position = POINT(?, ?), bounds = POINT(?, ?) WHERE id = ?");

	private static final PreparedStatement DELETE_ITEM = Database.prep("DELETE FROM restaurant_layout WHERE id = ?");
	
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
	
	private Map<Integer, Item> items;
	private int width, height;
	
	private RestaurantLayout() {
		this.items = new HashMap<>();
		
		// don't let width/height == 0
		this.width = 1;
		this.height = 1;
	}
	
	private void queryItems() {
		this.items = new HashMap<>();
		
		Database.query(result -> {
			Item item = Item.from(result);
			this.items.put(item.getId(), item);
			
			// check/update layout bounds
			updateBounds(item);
		}, QUERY_LAYOUT);
	}
	
	public JsonNode toJSON(LayoutJsonParams param) {
		JsonBuilder builder = JsonBuilder.create()
			.append("width", width)
			.append("height", height);

		builder.newArray("items");
		for(Item item : items.values()) {
			builder.append(item.toJSON(param));
		}
		
		builder.end();
		return builder.build();
	}
	
	public void recalculateSize() {
		// recalculate layout bounds
		this.width = 1;
		this.height = 1;
		
		for(Item item : this.items.values()) {
			updateBounds(item);
		}
	}
	
	private void updateBounds(Item item) {
		// check/update layout bounds
		int limit_x = item.location.x + item.bounds.width;
		int limit_y = item.location.y + item.bounds.height;
		
		if(limit_x > width) this.width = limit_x;
		if(limit_y > height) this.height = limit_y;
	}
	
// ============================================ Item Actions ============================================ \\
	
	public Item getItem(int id) {
		return items.computeIfAbsent(id, Item::new);
	}
	
	public Item newItem(int rotation, Point position, Dimension bounds, Table table) {
		Item item = new Item(rotation, position, bounds, table);
		items.put(item.getId(), item);
		
		// check/update layout bounds
		updateBounds(item);
		
		return item;
	}
	
	public boolean deleteItem(int id) {
		Item rem = items.get(id);
		// if item doesn't exist, job done
		if(rem == null) return true;
		
		// delete item
		if(!rem.delete()) return false;
		items.remove(id);
		
		recalculateSize();
		return true;
	}
	
// ============================================ =============== ============================================ \\
// ============================================ Restaurant Item ============================================ \\
	
	/**
	 * 	Represents a wall or table to be draw in restaurant view. <br />
	 * 	{@code table} will be {@code null} if Item is a wall
	 */
	public static class Item extends DatabaseObject {
		private Point location;
		private Dimension bounds;
		private int rotation;
		
		private Table table;
		
		private Item(int id) {
			super(id);
		}

		private Item(int rotation, Point position, Dimension bounds, Table table) {
			this.rotation = rotation;
			this.location = position;
			this.bounds = bounds;
			
			this.table = table;

			insert(); // insert into database
			query(); // re-query fields
		}
		
		private Item() {}

		static Item from(ResultSet result) throws SQLException {
			Item item = new Item();
			item.loadFromRow(result);
			return item;
		}

		// =========================================== DB - Object =========================================== \\
		
		public void loadFromRow(ResultSet result) throws SQLException {
			this.id = result.getInt("id");
			
			this.location = new Point(result.getInt("pos_x"), result.getInt("pos_y"));
			this.bounds = new Dimension(result.getInt("width"), result.getInt("height"));
			this.rotation = result.getInt("rotation");
			
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
					rotation,
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
		
		protected boolean delete() {
			LOG.warn("Deleting restaurant-item #{}...", id);
			int id = super.id;
			super.id = 0;
			
			return Database.update(DELETE_ITEM, id);
		}
		
// =========================================== Update Actions =========================================== \\
		
		public boolean updateConstraints(int rotation, Point position, Dimension bounds) {
			this.location = position;
			this.bounds = bounds;

			LOG.trace("Update layout-item #{} -- pos: {}, bounds: {}", id, position, bounds);			
			return Database.update(UPDATE_ITEM, rotation, position.x, position.y, bounds.width, bounds.height, id);
		}
		
// =========================================== JSON =========================================== \\
		
		public JsonNode toJSON(LayoutJsonParams param) {
			JsonBuilder builder = JsonBuilder.create()
				.append("id", id)
				.newObject("position")
					.append("x", location.x)
					.append("y", location.y)
				.end()
				.newObject("bounds")
					.append("width", bounds.width)
					.append("height", bounds.height)
				.end()
				.append("rotation", rotation);
			
			if(table != null) {
				builder.append("table", table.toJSON(param));
			} else {
				builder.appendNull("table");
			}
			
			return builder.build();
		}
	}
}
