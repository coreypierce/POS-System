package edu.wit.se16.model.layout;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Shift;
import edu.wit.se16.model.Table;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class Section extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM sections WHERE ID = ?");	
	private static final PreparedStatement INSERT = Database.prep("INSERT INTO sections (shift_id, section_number) VALUES(?, ?)");
	private static final PreparedStatement DELETE = Database.prep("DELETE FROM sections WHERE id = ?");
	
	private static final PreparedStatement ASSIGN = Database.prep("UPDATE sections SET assignee_id = ? WHERE ID = ?");

	private static final PreparedStatement LOOKUP_SECTION = Database.prep(
			"SELECT id FROM sections WHERE shift_id = ? AND assignee_id = ? LIMIT 1");	
	
	private static final PreparedStatement CHECK_TABLE = Database.prep(
			"SELECT * FROM section_tables WHERE section_id = ? AND table_id = ?");
	
	private static final PreparedStatement QUERY_TABLES = Database.prep("SELECT table_id FROM section_tables WHERE section_id = ?");

	private static final PreparedStatement LOOKUP_SECTION_BY_TABLE = Database.prep(
			"SELECT section_id FROM section_tables WHERE table_id = ? AND section_id IN (SELECT id FROM sections WHERE shift_id = ?)");
	
	private static final PreparedStatement ADD_TABLE_ASSIGNMENT = Database.prep(
			"INSERT INTO section_tables (section_id, table_id) VALUES(?, ?)");

	private static final PreparedStatement REMOVE_TABLE_ASSIGNMENT = Database.prep(
			"DELETE FROM section_tables WHERE section_id = ? AND table_id = ?");
	
	private int number;
	private int shift_id;
	private Employee assignee;
	
	public Section(int id) {
		super(id);
	}
	
	public Section(Shift shift) {
		this.shift_id = shift.getId();
		this.number = shift.nextSectionNumber();

		insert(); // insert into database
		query(); // re-query fields
	}
	
	public static Section findSection(Shift shift, Employee employee) {
		// mutable integer
		AtomicInteger id = new AtomicInteger(0);
		// query section-id based on shift and employee
		Database.query(result -> id.set(result.getInt("id")), LOOKUP_SECTION, shift.getId(), employee.getId());
		// if no section was found
		return id.get() <= 0 ? null : new Section(id.get());
	}
	
	public static Section findSection(Shift shift, Table table) {
		// mutable integer
		AtomicInteger id = new AtomicInteger(0);
		// query section-id based on shift and table
		Database.query(results -> id.set(results.getInt("section_id")), LOOKUP_SECTION_BY_TABLE, table.getId(), shift.getId());
		// if no section was found
		return id.get() <= 0 ? null : new Section(id.get());
	}

// =========================================== DB - Object =========================================== \\
	
	protected boolean query() {
		return Database.query(results -> {
			this.number = results.getInt("section_number");
			// get shift-id, but don't query the object yet
			this.shift_id = results.getInt("shift_id");
			
			// current assignee could be null
			int assigneeId = results.getInt("assignee_id");
			if(assigneeId > 0) {
				this.assignee = new Employee(assigneeId);
			}
			
		}, QUERY, id);
	}

	protected boolean insert() {
		if(id != 0) { 
			LOG.error("Section has already been created!");
			return false; 
		}
		
		LOG.trace("Inserting new Seciton #{} in Shift #{}...", number, shift_id);
		
		if(Database.update(INSERT, shift_id, number)) {
			try {
				// grab last generated id
				ResultSet prev_key = INSERT.getGeneratedKeys();
				if(!prev_key.next()) throw new SQLException("Failed to get section-id; no rows returned!");
				this.id = prev_key.getInt(1);

				return true;
				
			} catch (SQLException e) {
				LOG.error("An error occured while getting section-id", e);
				return false;
			}
		} else {
			LOG.warn("Section INSERT failed!");
			return false;
		}
	}
	
	public void delete() {
		LOG.trace("Deleting Section #{}...", super.id);
		Database.update(DELETE, super.id);
	}
	
// =========================================== Section Tables =========================================== \\
	
	public List<Table> getTables() {
		ArrayList<Table> tables = new ArrayList<>();
		
		Database.query(results -> {
			// query table by ID
			tables.add(new Table(results.getInt("table_id")));
		}, QUERY_TABLES, super.id);
		
		return tables;
	}
	
	public List<Integer> getTablesIds() {
		ArrayList<Integer> tables = new ArrayList<>();
		
		Database.query(results -> {
			// query table by ID
			tables.add(results.getInt("table_id"));
		}, QUERY_TABLES, super.id);
		
		return tables;
	}
	
	public boolean hasTable(Table table) {
		return Database.query(null, CHECK_TABLE, super.id, table.getId());
	}
	
	public void addTable(Table table) {
		LOG.trace("Adding Table #{} to Section #{}...", table.getId(), id);
		Database.update(ADD_TABLE_ASSIGNMENT, super.id, table.getId());
	}
	
	public void removeTable(Table table) {
		LOG.trace("Removing Table #{} from Section #{}...", table.getId(), id);
		Database.update(REMOVE_TABLE_ASSIGNMENT, super.id, table.getId());
	}
	
// =========================================== JSON =========================================== \\
	
	public JsonNode toJSON() {
		List<Integer> tableIDs = getTablesIds();
		
		JsonBuilder builder = JsonBuilder.create()
			.append("id", super.id)
			.append("number", this.number)
			.append("assignee", assignee == null ? null : assignee.toJSON());
		
		builder.newArray("tables");
		for(int table_id : tableIDs) {
			builder.append(table_id);
		}
		
		builder.end();
		return builder.build();
	}
	
// =========================================== Shift Access =========================================== \\
	
	public Shift getShift() {
		return new Shift(shift_id);
	}
	
// =========================================== Assignee Functions =========================================== \\
	
	public Employee getAssignee() {
		return assignee;
	}
	
	public void assignTo(Employee assignee) {
		this.assignee = assignee;
		Database.update(ASSIGN, assignee == null ? null : assignee.getId(), super.id);
	}

// =========================================== Section Number =========================================== \\
	
	public int getSectionNumber() { return number; }
}
