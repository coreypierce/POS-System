package root.api.test;

import java.sql.PreparedStatement;

import edu.wit.se16.database.Database;

public class Employee {
	private static final PreparedStatement createStatement = Database.prep("INSERT INTO EMPLOYEES (firstname,lastname,password_hash,password_salt)  VALUES(?,?,'','')");
	public Employee(String firstName, String lastName) {
		createEmployee(firstName,lastName);
	}
	private static void createEmployee(String firstName, String lastName) {
		Database.update(createStatement, firstName, lastName);
	}
}
