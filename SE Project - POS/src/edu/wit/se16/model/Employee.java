package edu.wit.se16.model;

import java.sql.PreparedStatement;

import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.security.PasswordUtil;
import edu.wit.se16.security.PasswordUtil.Password;
import edu.wit.se16.system.logging.LoggingUtil;

public class Employee extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM employees WHERE ID = ?");
	private static final PreparedStatement INSERT = Database.prep(
			"INSERT INTO employees (firstname, lastname, role, password_hash, password_salt) VALUES (?, ?, ?, ?, ?)");

	// prevents more the on instance from inserting at once
	private static final Object INSERT_LOCK = new Object();
	private static final PreparedStatement GRAB_LAST_ID = Database.prep(
			"SELECT id FROM employees ORDER BY created_on DESC LIMIT 1");
	
	private static final PreparedStatement DELETE = Database.prep("UPDATE employees SET deleted = true WHERE id = ?");
	
	public static enum Role {
		Server, Host, Manager;
	}
	
	private String firstname;
	private String lastname;
	
	private Role role;
	
	private String password_hash;
	private String password_salt;
	
	private boolean active, deleted;
	
	public Employee(int id) {
		super(id);
	}
	
	public Employee(String firstname, String lastname, Role role) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.role = role;
		
		Password password = PasswordUtil.generatePassword();
		this.password_hash = password.hash;
		this.password_salt = password.salt;
		
		// TODO: display / provide password.password to manager...
		
		insert(); // insert into database
		query(); // re-query fields
		
		// TODO: DELETE
		LOG.debug("Employee #{} password \"{}\"", id, password.plain_text);
	}

// =========================================== DB - Object =========================================== \\
	
	protected boolean query() {
		return Database.query(result -> {
			this.firstname = result.getString("firstname");
			this.lastname = result.getString("lastname");

			this.role = Role.valueOf(result.getString("role"));
			
			this.password_hash = result.getString("password_hash");
			this.password_salt = result.getString("password_salt");
			
			this.active = result.getBoolean("active");
			this.deleted = result.getBoolean("deleted");
			
		}, QUERY, id);
	}
	
	protected boolean insert() {
		if(id != 0) { 
			LOG.error("Employee has already been created!");
			return false; 
		}
		
		LOG.trace("Inserting new {}({}, {}) to employees...", role, firstname, lastname);
		
		// prevent multiple insertion attempts at once
		synchronized (INSERT_LOCK) {
			if(Database.update(INSERT, firstname, lastname, role.toString(), password_hash, password_salt)) {
				
				// get the employee-id, based on last row inserted into table (hence the lock)
				Database.query(result -> {
					this.id = result.getInt("id");
				}, GRAB_LAST_ID);
				
				return true;
				
			} else {
				LOG.warn("Employee INSERT failed!");
				return false;
			}
		}
	}

// =========================================== Update State =========================================== \\

	public void delete() {
		if(deleted) { LOG.warn("Employee has already been deleted!"); return; }
		
		LOG.trace("Deleting Employee #{} ...", id);
		Database.update(DELETE, id);
	}

// =========================================== Password =========================================== \\
	
	public String getSalt() { return password_salt; }
	public String getPassword() { return password_hash; }
}
