package edu.wit.se16.model;

import java.sql.PreparedStatement;
import java.util.ArrayList;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.Database;
import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.security.PasswordUtil;
import edu.wit.se16.security.PasswordUtil.Password;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class Employee extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final PreparedStatement QUERY_ALL_IDS = Database.prep("SELECT id FROM employees WHERE deleted = false");
	
	private static final PreparedStatement QUERY = Database.prep("SELECT * FROM employees WHERE ID = ?");
	private static final PreparedStatement INSERT = Database.prep(
			"INSERT INTO employees (firstname, lastname, role, password_hash, password_salt) VALUES (?, ?, ?, '', '')");

	private static final PreparedStatement CHANGE_PASSWORD = Database.prep(
			"UPDATE employees SET password_hash = ?, password_salt = ? WHERE id = ?");

	// prevents more the on instance from inserting at once
	private static final Object INSERT_LOCK = new Object();
	private static final PreparedStatement GRAB_LAST_ID = Database.prep(
			"SELECT id FROM employees ORDER BY created_on DESC LIMIT 1");
	
	private static final PreparedStatement DELETE = Database.prep("UPDATE employees SET deleted = true WHERE id = ?");
	private static final PreparedStatement ACTIVATE = Database.prep("UPDATE employees SET active = true WHERE id = ?");
	private static final PreparedStatement DEACTIVATE = Database.prep("UPDATE employees SET active = false WHERE id = ?");
	
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
		
		insert(); // insert into database
		query(); // re-query fields
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
			if(Database.update(INSERT, firstname, lastname, role.toString())) {
				
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
	
	public static Employee[] getAllEmployees() {
		ArrayList<Employee> employees = new ArrayList<>();
		
		Database.query(result -> {
			employees.add(new Employee(result.getInt("id")));
		}, QUERY_ALL_IDS);
		
		return employees.toArray(new Employee[0]);
	}
	
// =========================================== Update State =========================================== \\

	public void delete() {
		if(deleted) { LOG.warn("Employee has already been deleted!"); return; }
		
		LOG.trace("Deleting Employee #{} ...", id);
		Database.update(DELETE, id);
		
		this.deleted = true;
		SessionToken.clearEmplyeesSession(this);
	}
	
	public void activate() {
		if(active) { LOG.warn("Employee is already active!"); return; }
		
		LOG.trace("Activating Employee #{} ...", id);
		Database.update(ACTIVATE, id);
		
		this.active = true;
	}
	
	public void deactivate() {
		if(!active) { LOG.warn("Employee is already deactivated!"); return; }
		
		LOG.trace("Deactivating Employee #{} ...", id);
		Database.update(DEACTIVATE, id);
		
		this.active = false;
	}

// =========================================== Adjust Password =========================================== \\
	
	public String resetPassword() {
		LOG.debug("Resetting the password for Employee #{}...", id);
		
		Password password = PasswordUtil.generatePassword();
		this.password_hash = password.hash;
		this.password_salt = password.salt;
		
		Database.update(CHANGE_PASSWORD, this.password_hash, this.password_salt, super.id);
		// clear session, requiring new login
		SessionToken.clearEmplyeesSession(this);
		return password.plain_text;
	}

	public void setPassword(String raw_password) {
		LOG.trace("Changing Employee #{}'s password...", id);
		
		Password password =  PasswordUtil.generatePassword(raw_password);
		this.password_hash = password.hash;
		this.password_salt = password.salt;

		LOG.trace("Submitting Employee #{}'s password to database...", id);
		Database.update(CHANGE_PASSWORD, this.password_hash, this.password_salt, super.id);
		// clear session, requiring new login
		SessionToken.clearEmplyeesSession(this);
	}
	
// =========================================== Password =========================================== \\
	
	public String getSalt() { return password_salt; }
	public String getPassword() { return password_hash; }

// =========================================== JSON =========================================== \\
	
	public JsonNode toJSON() {
		return JsonBuilder.create()
			.append("id", super.id)
			
			.append("firstname", firstname)
			.append("lastname", lastname)
			.append("role", role.toString())
			
			.append("active", active)
		.build();
	}
	
// =========================================== Getters =========================================== \\
	
	public String getFirstName() { return firstname; }
	public String getLastName() { return lastname; }
	
	public Role getRole() { return role; }
}
