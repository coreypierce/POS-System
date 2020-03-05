package root.api.test;

import java.sql.PreparedStatement;

import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.system.logging.LoggingUtil;

public class Employee {
	private static final PreparedStatement createStatement = Database.prep("INSERT INTO EMPLOYEES (firstname,lastname,password_hash" +
			"password_salt)  VALUES(?,?,'','')");
	private static final PreparedStatement listEmployee = Database.prep("SELECT * FROM EMPLOYEES");
	private static final Logger LOG = LoggingUtil.getLogger();
	//activate/deactivate statement 
	//delete employee statement
	//set password statement
	public Employee() {
		
	}
	public Employee(String firstName, String lastName) {
		createEmployee(firstName,lastName);
	}
	private static void createEmployee(String firstName, String lastName) {
		Database.update(createStatement, firstName, lastName);
	}
	public static void listEmployees() {
		LOG.trace("id\tfirstname\tlistname\trole");
		Database.query(results -> {
			LOG.trace("{}\t{}\t{}\t{}", results.getInt("id"), results.getString("firstname"), 
					results.getString("lastname"), results.getString("role"));
		}, listEmployee);
	}
}
