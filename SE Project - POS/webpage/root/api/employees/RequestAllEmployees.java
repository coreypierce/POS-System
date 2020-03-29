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

public class RequestAllEmployees implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Role query_role = request.getBody("role", Role::valueOf, null);
		Boolean active = request.getBody("active", Boolean::parseBoolean, null);
		
		Employee manager = SessionManager.getSessionToken().getEmployee();
		LOG.trace("Employee #{} is requesting all employee information", manager.getId());
		
		// validate user is a Manager
//		if(manager.getRole() != Role.Manager) {
//			return StandardResponses.error(request, response, 
//					HttpServletResponse.SC_FORBIDDEN, "Only Managers can access employee info!");
//		}
		
		JsonBuilder builder = JsonBuilder.create()
			.newArray("employees");
		
		// write all employee's into JSON
		Employee[] employees = Employee.getAllEmployees(query_role, active);
		for(Employee employee : employees) {
			builder.append(employee.toJSON());
		}
		
		// end array and send to client
		builder.end();
		builder.build(response);

		// respond OK
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "list"; }
}