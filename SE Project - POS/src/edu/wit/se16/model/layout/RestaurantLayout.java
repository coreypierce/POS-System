package edu.wit.se16.model.layout;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.model.Table;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RestaurantLayout {
	private static final Logger LOG = LoggingUtil.getLogger();
	
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
		// TODO: load from database
		INSTANCE = new RestaurantLayout();
		LOG.warn("TODO: Load Restaurant-Layout from Database");
		
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
	
	public JsonNode toJSON() {
		JsonBuilder builder = JsonBuilder.create()
			.append("width", width)
			.append("heihgt", height);

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
	public static class Item {
		private Point location;
		private Dimension bounds;
		
		private Table table;
		
		
		
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
			
			if(table == null) {
				builder.newObject("table")
//					.append("id", table.getID())
					// TODO: Table details and descriptor
				.end();
				
			} else {
				builder.appendNull("table");
			}
			
			return builder.build();
		}
	}
}
