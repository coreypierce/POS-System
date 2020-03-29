package root.api.layout.section;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.model.layout.Section;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestAssignSection implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer section_id = request.getBody("section_id", Integer::parseInt, null);
		Integer employee_id = request.getBody("assignee_id", Integer::parseInt, null);
		
		// validate inputs
		if(section_id == null) {
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'section_id'");
		}
		
		if(employee_id == null) {
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'assignee_id'");
		}
		
		LOG.trace("Assigning Section #{} to Employee #{}...", section_id, employee_id);

		Section section;
		Employee assignee;
		
		try {
			assignee = new Employee(employee_id);
			if(assignee.getRole() != Role.Server) {
				return StandardResponses.error(request, response, 
						HttpServletResponse.SC_BAD_REQUEST, "Sections can only be assigned to Servers!");
			}
					
		} catch(NoSuchElementException e) {
			LOG.warn("Attempted to assign Section #{} to a non-exciting Employee!", section_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Could not find the specified employee!");
		}
		
		try {
			section = new Section(section_id);
					
		} catch(NoSuchElementException e) {
			LOG.warn("no such section Section #{}!", section_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Could not find the specified Section!");
		}
		
		// assignee the section and return
		section.assignTo(assignee);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "assign"; }
}
