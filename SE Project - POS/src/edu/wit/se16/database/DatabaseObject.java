package edu.wit.se16.database;

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

	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result + id;
		
		return result;
	}

	public boolean equals(Object obj) {
		if(this == obj) return true;
		if(obj == null) return false;
		if(!(obj instanceof DatabaseObject))
			return false;
		
		DatabaseObject other = (DatabaseObject) obj;
		if (id != other.id)
			return false;
		
		return true;
	}
}
