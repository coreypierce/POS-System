package root.api.employees;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestNewEmployee implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		String firstname = request.getBody("firstname");
		String lastname = request.getBody("lastname");
		String role_raw = request.getBody("role");
		
		// validate parameters
		
		if(firstname == null || lastname == null || role_raw == null) {
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Missing parameter values!");
		}
		
		if(firstname.isEmpty() || lastname.isEmpty() || role_raw.isEmpty()) {
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Parameter values cannot be blank!");
		}
		
		Role role;
		try { role = Role.valueOf(role_raw); }
		catch(IllegalArgumentException e) {
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Invalid role-type!");
		}
		
		Employee manager = SessionManager.getSessionToken().getEmployee();
		LOG.trace("Employee #{} is requesting a new Employee be created...", manager.getId());
		
		// validate user is a Manager
		if(manager.getRole() != Role.Manager) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_FORBIDDEN, "Only Managers can access employee info!");
		}
		
		// create employee
		LOG.trace("New employee has been requested with: Name = {} {}, Role = {}", firstname, lastname, role);
		Employee employee = new Employee(firstname, lastname, role);

		// reset employee password
		LOG.trace("Employee #{} has been created! Resetting password...", employee.getId());
		String temp_password = employee.resetPassword();
		
		// send-back employee and temp-password
		JsonBuilder.create()
			.append("employee", employee.toJSON())
			.append("password", temp_password)
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "new"; }
}
