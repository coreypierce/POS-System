package root.api.employees;

import java.io.IOException;
import java.util.NoSuchElementException;

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

public class RequestActivate implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		int id = request.getBody("id", Integer::parseInt, 0);
		
		// validate parameters
		
		if(id <= 0) {
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Invalid employee-id!");
		}
		
		Employee manager = SessionManager.getSessionToken().getEmployee();
		LOG.trace("Employee #{} is requesting to Re-activating Employee #{}...", manager.getId(), id);
		
		// validate user is a Manager
		if(manager.getRole() != Role.Manager) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_FORBIDDEN, "Only Managers can access employee info!");
		}
		
		try {
			Employee employee = new Employee(id);
			employee.activate();
			
		} catch(NoSuchElementException e) {
			LOG.warn("Could not find Employee #{}!", id);
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "No employee with ID exists!");
		}

		// respond OK
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "activate"; }
}