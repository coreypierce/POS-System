package root.api.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;

import com.mysql.fabric.xmlrpc.base.Data;

import edu.wit.se16.database.Database;
import edu.wit.se16.system.logging.LoggingUtil;

public class Employee {
	private static final PreparedStatement createStatement = Database.prep("INSERT INTO EMPLOYEES (firstname,lastname,password_hash," +
			"password_salt)  VALUES(?,?,'','')");
	private static final PreparedStatement listEmployee = Database.prep("SELECT * FROM EMPLOYEES");
	private static final PreparedStatement removeEmployee = Database.prep("DELETE FROM EMPLOYEES WHERE id = ?");
	private static final PreparedStatement activateEmployee = Database.prep("UPDATE EMPLOYEES SET active = 1, deleted = 0 WHERE id = ?");
	private static final PreparedStatement deactivateEmployee = Database.prep("UPDATE EMPLOYEES SET active = 0, deleted = 1 WHERE id = ?");
	private static final Logger LOG = LoggingUtil.getLogger();
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
	public static void createEmployeeReport() {
		Date date = Calendar.getInstance().getTime();	//create date object
		DateFormat reportDate = new SimpleDateFormat("yyyy-MM-dd");	//format date in certain way
		String reportName = reportDate.format(date);	//use date format as file name
		File employeeReport = new File(reportName + ".csv");	//creating the file
		/*
		 * Writing to file section
		 */
		try(FileWriter fw = new FileWriter(employeeReport)){
			fw.write("id,firstname,lastname,role\n");
			//query through DB and write the output to a csv file
			Database.query(results ->{
				try {
					fw.write(results.getInt("id") + "," + results.getString("firstname") + "," 
							+ results.getString("lastname") + "," + results.getString("role") + "\n");
				}catch (IOException e) {
					LOG.error("File Error!!",e);
				}
			}, listEmployee);
		} catch (IOException e) {
			LOG.error("File Error!!",e);
		}
	}
	public static void removeEmployee(int employeeID) {
		Database.update(removeEmployee, employeeID);
	}
	public static void activateEmployee(int employeeID) {
		Database.update(activateEmployee, employeeID);
	}
	public static void deactivateEmployee(int employeeID) {
		Database.update(deactivateEmployee, employeeID);
	}
}
