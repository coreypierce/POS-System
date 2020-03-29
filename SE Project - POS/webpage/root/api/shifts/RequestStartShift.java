package root.api.shifts;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.model.Shift;
import edu.wit.se16.model.Shift.ShiftType;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestStartShift implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		ShiftType type = request.getBody("type", ShiftType::valueOf, null);
		Integer manager_id = request.getBody("manager_id", Integer::parseInt, null);
		
		// validate parameters
		if(type == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing or bad shift 'type'");
		}
		
		if(manager_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing 'manager_id'");
		}
		
		LOG.trace("Request to start new '{}' Shift wtih Manager #{}...", type, manager_id);
		Employee manager;
		
		try {
			// lookup manager by employee-id
			manager = new Employee(manager_id);
			
			// make sure the employee is actually a Manager
			if(manager.getRole() != Role.Manager) {
				throw new NoSuchElementException("Employee #" + manager_id + " is not a Manager");
			}
			
		} catch(NoSuchElementException e) {
			LOG.error("Requested Manager #{} doesn't none exists!", manager_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Specified Manager does not exist!");
		}
		
		// create a new Shift
		Shift shift = new Shift(type, manager);
		
		JsonBuilder.create()
			.append("shift_id", shift.getId())
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "start"; }
}