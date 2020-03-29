package edu.wit.se16.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.model.layout.Section;
import edu.wit.se16.system.logging.LoggingUtil;

public class Shift extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();

	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM shifts WHERE ID = ?");	
	private static final PreparedStatement INSERT = Database.prep("INSERT INTO shifts (shift_type, manager_id) VALUES(?, ?)");
	
	private static final PreparedStatement QUERY_CURRENT_SHIFT = Database.prep("SELECT id FROM shifts ORDER BY date DESC LIMIT 1");
	
	private static final PreparedStatement LIST_ALL_SECTIONS = Database.prep("SELECT id FROM sections WHERE shift_id = ?");

	private static final PreparedStatement NEXT_SECTION_NUMBER = Database.prep(
			"(SELECT (section_number + 1) as next_number FROM sections t1 WHERE NOT EXISTS(SELECT section_number FROM sections"
			+ " WHERE section_number = t1.section_number + 1 AND shift_id = t1.shift_id LIMIT 1) AND shift_id = ? LIMIT 1)"
			+ " UNION DISTINCT (SELECT 1 as next_number FROM sections WHERE NOT EXISTS(SELECT section_number FROM sections "
			+ " WHERE section_number = 1 AND shift_id = ?) LIMIT 1) ORDER BY next_number ASC LIMIT 1");
	
	public static enum ShiftType {
		Morning, Noon, Evening, Night;
	}
	
	private int manager_id;
	private ShiftType type;
	
	public Shift(int id) {
		super(id);
	}
	
	public Shift(ShiftType type, Employee manager) {
		this.manager_id = manager.getId();
		this.type = type;
		
		insert(); // insert into database
		query(); // re-query fields
	}
	
	public static Shift getCurrentShift() {
		// mutable integer
		AtomicInteger id = new AtomicInteger(0);
		// query the newest shift-id based on start datatime
		Database.query(result -> { id.set(result.getInt("id")); }, QUERY_CURRENT_SHIFT);
		// if no section was found
		return id.get() == 0 ? null : new Shift(id.get());
	}

// =========================================== DB - Object =========================================== \\
	
	protected boolean query() {
		return Database.query(results -> {
			this.type = ShiftType.valueOf(results.getString("shift_type"));
			this.manager_id = results.getInt("manager_id");
			
		}, QUERY, id);
	}

	protected boolean insert() {
		if(id != 0) { 
			LOG.error("Section has already been created!");
			return false; 
		}
		
		LOG.trace("Inserting new {}-Shift with Manager #{}...", type, manager_id);
		
		if(Database.update(INSERT, type.toString(), manager_id)) {
			try {
				// grab last generated id
				ResultSet prev_key = INSERT.getGeneratedKeys();
				if(!prev_key.next()) throw new SQLException("Failed to get shift-id; no rows returned!");
				this.id = prev_key.getInt(1);

				return true;
				
			} catch (SQLException e) {
				LOG.error("An error occured while getting shift-id", e);
				return false;
			}
		} else {
			LOG.warn("Shift INSERT failed!");
			return false;
		}
	}

// =========================================== Sections =========================================== \\
	
	public List<Section> getAllSections() {
		ArrayList<Section> sections = new ArrayList<>();
		
		Database.query(results -> {
			sections.add(new Section(results.getInt("id")));
		}, LIST_ALL_SECTIONS, super.id);
		
		return sections;
	}
	
	public int nextSectionNumber() { 
		LOG.trace("Calculating next section-number...");
		
		AtomicInteger nextNumber = new AtomicInteger(1);
		Database.query(results -> nextNumber.set(results.getInt("next_number")), NEXT_SECTION_NUMBER, super.id, super.id);
		return nextNumber.get();
	}
}
