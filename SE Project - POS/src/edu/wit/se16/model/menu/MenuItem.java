package edu.wit.se16.model.menu;

import com.fasterxml.jackson.databind.JsonNode;

import edu.wit.se16.database.DatabaseObject;

public class MenuItem extends DatabaseObject {
	
	public MenuItem(int id) {
		super(id);
	}
	
	@Override
	protected boolean query() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean insert() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public JsonNode toJSON() {
		return null;
	}
	
	

}
