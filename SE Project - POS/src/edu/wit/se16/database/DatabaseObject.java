package edu.wit.se16.database;

import java.sql.PreparedStatement;
import java.util.NoSuchElementException;

public abstract class DatabaseObject {
	protected int id;
	
	public DatabaseObject(int id) {
		this.id = id;
		
		if(!query()) {
			throw new NoSuchElementException();
		}
	}
	
	public DatabaseObject() {}
	
	/** Loads values from database into Object */
	protected abstract boolean query();
	/** Inserts new instance into database */
	protected abstract boolean insert();
	
	public int getId() { return id; }
}
