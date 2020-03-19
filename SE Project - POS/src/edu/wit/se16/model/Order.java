package edu.wit.se16.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.model.menu.MenuItem;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class Order extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM orders WHERE ID = ?");
	private static final PreparedStatement INSERT = Database.prep("INSERT INTO orders (server_id, table_id) VALUES (?, ?)");

	private static final PreparedStatement QUERY_BY_TABLE = Database.prep(
			"SELECT id FROM orders WHERE table_id = ? ORDER BY timestamp DESC LIMIT 1");
	
	private static final PreparedStatement QUERY_ALL_ITEM = Database.prep("SELECT * FROM order_items WHERE order_id = ?");
	private static final PreparedStatement DELETE_ITEM = Database.prep("DELETE order_items WHERE order_id = ? AND item_id = ?");

	private static final PreparedStatement INSERT_ITEM = Database.prep(
			"INSERT INTO order_items (order_id, item_id, quantity) VALUES(?, ?, ?)");
	
	private static final PreparedStatement UPDATE_ITEM_QUANTITY = Database.prep(
			"UPDATE order_items SET quantity = ? WHERE order_id = ? AND item_id = ?");

	private int server_id;
	private int table_id;
	
	private Map<MenuItem, OrderItem> items;
	
	public Order(int id) {
		super(id);
	}
	
	public Order(Employee server, Table table) {
		this.server_id = server.getId();
		this.table_id = table.getId();
		
		this.items = new HashMap<>();
		
		insert(); // insert into database
		query(); // re-query fields
	}
	
	public static Order getTablesOrder(int table_id) {
		AtomicInteger id = new AtomicInteger();
		Database.query(result -> { id.set(result.getInt("id")); }, QUERY_BY_TABLE, table_id);
		return id.get() > 0 ? new Order(id.get()) : null;
	}
	
	// =========================================== DB - Object =========================================== \\
	
	protected boolean query() {
		return Database.query(result -> {
			this.server_id = result.getInt("server_id");
			this.table_id = result.getInt("table_id");
			
			this.items = new HashMap<>();
			
			// query all items
			Database.query(item_results -> {
				OrderItem item = OrderItem.from(this, item_results);
				this.items.put(item.item, item);
			}, QUERY_ALL_ITEM, id);
			
		}, QUERY, id);
	}
	
	protected boolean insert() {
		if(id != 0) { 
			LOG.error("Order has already been created!");
			return false; 
		}
		
		LOG.trace("Inserting new Order for Table #{}...", table_id);
		// call update request
		if(Database.update(INSERT, server_id, table_id)) {
			try {
				// grab last generated id
				ResultSet prev_key = INSERT.getGeneratedKeys();
				if(!prev_key.next()) throw new SQLException("Failed to get order-id; no rows returned!");
				this.id = prev_key.getInt(1);

				return true;
				
			} catch (SQLException e) {
				LOG.error("An error occured while getting order-id", e);
				return false;
			}
		} else {
			LOG.warn("Order INSERT failed!");
			return false;
		}
	}
	
	// =========================================== Manage Item =========================================== \\
	
	public void setItemAmount(MenuItem item, int amount) {
		LOG.trace("Adjusting Order #{}, setting Menu-Item #{} to {}...", super.id, item.getId(), amount);
		
		if(items.containsKey(item)) {
			this.items.get(item).updateQuantity(amount);

		} else {
			// create / insert new Order-Item
			this.items.put(item, new OrderItem(this, item, amount));
		}
	}
	
	public void removeItem(MenuItem item) {
		LOG.trace("Removing Menu-Item #{} from Order #{}...", item.getId(), super.id);
		
		this.items.remove(item);
		Database.update(DELETE_ITEM, super.id, item.getId());
	}
	
	public void update(ArrayList<Map<String, Object>> raw) {
		Set<MenuItem> toRem = this.items.keySet();
		
		for(Map<String, Object> raw_item : raw) {
			int item_id = (Integer) raw_item.get("item");
			int amount = (Integer) raw_item.get("quantity");

			try {
				MenuItem menuItem = new MenuItem(item_id);
				setItemAmount(menuItem, amount);
				toRem.remove(menuItem);
			
			} catch(NoSuchElementException e) {
				LOG.warn("No such Menu-Item #{}! Skipping items...", item_id);
			}
		}
		
		for(MenuItem menuItem : toRem) {
			removeItem(menuItem);
		}
	}

	// =========================================== JSON =========================================== \\
	
	public JsonNode toJSON() {
		JsonBuilder builder = JsonBuilder.create();
		builder.append("id", super.id);
		
		builder.newArray("items");
		for(OrderItem item : items.values()) {
			item.appendJSON(builder);
		}
		
		builder.end();
		return builder.build();
	}
	
	// =========================================== Order-Item =========================================== \\
	
	private static class OrderItem {
		private Order order;
		private MenuItem item;
		private int quantity;
		
		private OrderItem(Order order, MenuItem item, int amount) {
			this.order = order;
			this.item = item;
			this.quantity = amount;

			insert(); // insert into database
		}
		
		private OrderItem() { }
		
		public static OrderItem from(Order order, ResultSet result) throws SQLException {
			OrderItem item = new OrderItem();
			
			item.order = order;
			item.item = new MenuItem(result.getInt("item_id"));
			item.quantity = result.getInt("quantity");
			
			return item;
		}

		// =========================================== DB - Item =========================================== \\
		
		public void insert() {
			LOG.trace("Inserting new Order-Item({}, {})...", order.id, item.getId());
			// call update request
			if(!Database.update(INSERT_ITEM, order.id, item.getId(), quantity)) {
				LOG.warn("Order-Item INSERT failed!");
			}
		}
		
		public void updateQuantity(int quantity) {
			LOG.trace("Setting Order-Item({}, {}) quantity to {}", order.id, item.getId(), quantity);
			
			this.quantity = quantity;
			Database.update(UPDATE_ITEM_QUANTITY, quantity, order.id, item.getId());
		}
		
		// =========================================== Data Access =========================================== \\
		
		public void appendJSON(JsonBuilder builder) {
			builder.newObject()
				.append("item", item.toJSON())
				.append("quantity", quantity)
			.end();
		}
	}
}
