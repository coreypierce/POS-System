package edu.wit.se16.networking;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.map.CaseInsensitiveMap;

import edu.wit.se16.database.Database;
import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.model.Shift;
import edu.wit.se16.model.Shift.ShiftType;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.networking.requests.RequestPage;
import edu.wit.se16.networking.requests.loaders.HTMLResourceLoader;

public class SetupHandler {
	private static final int ROOT_MANAGER_ID = 9000000;
	
	private static boolean isSetupComplete = false;
	
	public static boolean shouldEnterSetup() {
		if(isSetupComplete) return false;
		
		Database.query(
				results -> isSetupComplete = results.getInt("amount") > 0,
				Database.prep("SELECT COUNT(id) as amount FROM employees")); // one off statement
		
		return !isSetupComplete;
	}
	
	public static void runSetup(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		// create the root-employee
		Employee employee = new Employee("Root", "Manager", Role.Manager);
		String tempPassword = employee.resetPassword();
		
		// set their ID into the protected range
		Database.update(Database.prep("UPDATE employees SET id = ? WHERE id = ?"), ROOT_MANAGER_ID, employee.getId());
		
		// always force there to be a shift
		new Shift(ShiftType.Noon, employee);
		
		CaseInsensitiveMap values = new CaseInsensitiveMap();
		values.put("setup_id", ROOT_MANAGER_ID);
		values.put("setup_password", tempPassword);

		values.put("redirect_url", "/nav/edit");
		
		// open stream to HTML file, and open response stream
		InputStream in = HTMLResourceLoader.loadHTMLStream("root/pages/login/setup/setup.html", values);
		RequestPage.sendPage("root/pages/login/setup/setup.html", "setup-page", in, request, response);
	}
	
//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private SetupHandler() {}
}
