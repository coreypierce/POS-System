package edu.wit.se16.model;

import org.slf4j.Logger;

import edu.wit.se16.database.DatabaseObject;
import edu.wit.se16.system.logging.LoggingUtil;

public class Shift extends DatabaseObject {
	private static final Logger LOG = LoggingUtil.getLogger();

	public Shift(int shift_id) {
		// TODO Auto-generated constructor stub
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

	
	public int nextNumber() { 
		LOG.warn("TODO: generate section number");
		return 0; 
	}
}
