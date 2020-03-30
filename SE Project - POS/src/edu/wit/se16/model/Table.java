package edu.wit.se16.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.model.layout.LayoutJsonParams;
import edu.wit.se16.model.layout.Section;
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
	
	private static final PreparedStatement NEXT_TABLE_NUMBER = Database.prep(
			"SELECT (table_number + 1) as next_number FROM tables t1 WHERE NOT EXISTS(" +
					" SELECT table_number FROM tables t2 WHERE table_number = t1.table_number + 1 " +
					" AND EXISTS(SELECT id FROM restaurant_layout WHERE is_table = t2.id LIMIT 1)" +
					" LIMIT 1) " +
			"UNION DISTINCT (SELECT 1 as next_number FROM tables WHERE NOT EXISTS (SELECT table_number FROM tables t3" +
				" WHERE table_number = 1 AND EXISTS(SELECT id FROM restaurant_layout WHERE is_table = t3.id LIMIT 1))" +
				" LIMIT 1)" +
			" ORDER BY next_number ASC LIMIT 1");
	
	private static final Object sync_insert = new Object();
	
	public static enum TableStatus {
		Open, Seated, Order_Placed, Check_Printed, Check_In;
	}
	
	private int tableNumber;
	private String tableDescriptor;
	
	public Table(int id) {
		super(id);
	}
	
	public Table(String tableDescriptor) {
		synchronized(sync_insert) {
			this.tableNumber = Table.calculateNextTableNumber();
			this.tableDescriptor = tableDescriptor;
			
			insert(); // insert into database
			query(); // re-query fields
			
			setStatus(TableStatus.Open, null);
		}
	}

	private static int calculateNextTableNumber() {
		LOG.trace("Calculating next table-number...");
		
		AtomicInteger nextNumber = new AtomicInteger(1);
		Database.query(results -> nextNumber.set(results.getInt("next_number")), NEXT_TABLE_NUMBER);
		return nextNumber.get();
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
		
		setStatus(TableStatus.Order_Placed, employee);
		assignToEmployee(employee);
		
		return new Order(employee, this);
	}
	
	public double printCheck(Employee employee) {
		LOG.trace("Printing check for Table #{}", super.id);
		
		// get the current order for the table
		Order order = Order.getTablesOrder(super.id);
		if(order == null) throw new IllegalStateException("No order exists for Table #" + super.id);

		setStatus(TableStatus.Check_Printed, employee);
		return order.calculateBill();
	}
	
// =========================================== Section Functions =========================================== \\
	
	public void assignToEmployee(Employee employee) {
		LOG.trace("Assigning Table #{} to Employee #{}...", super.id, employee.getId());
		
		Shift shift = Shift.getCurrentShift();
		Section section = Section.findSection(shift, employee);
		
		// if this table is already part of the Employee's Section
		if(section.hasTable(this)) return;

		LOG.trace("Table #{} is not part of Employee #{}'s section!", super.id, employee.getId());
		section.addTempTable(this);
	}
	
// =========================================== Customer Function =========================================== \\

	public void seatCustomer(Employee employee, int amount) {
		LOG.trace("Seating {} Customer(s) at Table #{}", amount, super.id);
		setStatus(TableStatus.Seated, employee);
	}
	
	public void clearTable(Employee employee) {
		LOG.trace("Clearing Table #{}", super.id);
		setStatus(TableStatus.Open, employee);
		
		// back to open, so unassigns temp
		Section.removeTempTable(this);
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
		boolean can_pickup = false;
		
		// if a section was provided
		if(param != null && param.section != null) {
			// if the table is not part of that section
			if(!param.section.hasTable(this)) {
				can_pickup = status.equals(TableStatus.Seated.toString());
				// hide status of that table
				status = "unknown";
			}
		}
		
		return JsonBuilder.create()
			.append("id", super.id)
			.append("name", "TB-" + tableNumber)
			.append("icon", tableDescriptor)
			.append("status", status) 
			.append("can_pickup", can_pickup)
		.build();
	}
}
