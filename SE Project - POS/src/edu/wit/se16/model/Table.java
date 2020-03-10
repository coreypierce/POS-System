package edu.wit.se16.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.model.layout.LayoutJsonParams;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class Table extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM tables WHERE ID = ?");
	private static final PreparedStatement INSERT = Database.prep(
			"INSERT INTO tables (table_number, table_descriptor) VALUES (?, ?)");
	

	private static final PreparedStatement GET_STATUS = Database.prep(
			"SELECT status FROM table_status_history WHERE table_id = ? AND status != 'Check_In' ORDER BY timestamp DESC LIMIT 1");
	
	private static final PreparedStatement UPDATE_STATUS = Database.prep(
			"INSERT INTO table_status_history (employee_id, table_id, status) VALUES (?, ?, ?)");
	
	public static enum TableStatus {
		Open, Seated, Order_Placed, Check_Printed, Check_In;
	}
	
	private int tableNumber;
	private String tableDescriptor;
	
	public Table(int id) {
		super(id);
	}
	
	public Table(String tableDescriptor, int tableNumber) {
		this.tableNumber = tableNumber;
		this.tableDescriptor = tableDescriptor;
		
		insert(); // insert into database
		query(); // re-query fields
		
		setStatus(TableStatus.Open, null);
	}

// =========================================== DB - Object =========================================== \\
	
	protected boolean query() {
		return Database.query(result -> {
			this.tableNumber = result.getInt("table_number");
			this.tableDescriptor = result.getString("table_descriptor");
		}, QUERY, id);
	}
	
	protected boolean insert() {
		if(id != 0) { 
			LOG.error("Table has already been created!");
			return false; 
		}
		
		LOG.trace("Inserting new Table #{}...", tableNumber);
		// call update request
		if(Database.update(INSERT, tableNumber, tableDescriptor)) {
			try {
				// grab last generated id
				ResultSet prev_key = INSERT.getGeneratedKeys();
				if(!prev_key.next()) throw new SQLException("Failed to get table-id; no rows returned!");
				this.id = prev_key.getInt(1);

				return true;
				
			} catch (SQLException e) {
				LOG.error("An error occured while getting table-id", e);
				return false;
			}
		} else {
			LOG.warn("Table INSERT failed!");
			return false;
		}
	}

// =========================================== Order Function =========================================== \\
	
	public Order startOrder(Employee employee) {
		LOG.trace("Starting new order on Table #{}", super.id);
		return new Order(employee, this);
	}

// =========================================== Table Status =========================================== \\
	
	public TableStatus getStatus() {
		// used as a mutable-string
		StringBuffer status = new StringBuffer(15);
		
		Database.query(result -> {
			status.append(result.getString("status"));
		}, GET_STATUS, id);
		
		return TableStatus.valueOf(status.toString());
	}
	
	public void setStatus(TableStatus status, Employee employee) {
		// validate parameters
		if(status == null) throw new IllegalArgumentException("Table-Status cannot be 'null'");
		
		// attempt to insert new 'table_status_history' entry
		if(!Database.update(UPDATE_STATUS, employee == null ? null : employee.getId(), id, status.toString())) {
			LOG.error("Table-Status update failed!");
		}
	}
	
// =========================================== Display JSON =========================================== \\
	
	public JsonNode toJSON(LayoutJsonParams param) {
		// remove confusion on enum-translation by using string
		String status = getStatus().toString();
		
		// if a section was provided
		if(param != null && param.section != null) {
			// if the table is not part of that section
			if(!param.section.hasTable(this)) {
				// hide status of that table
				status = "unknown";
			}
		}
		
		return JsonBuilder.create()
			.append("id", super.id)
			.append("name", "TB-" + tableNumber)
			.append("icon", tableDescriptor)
			.append("status", status) 
		.build();
	}
}
