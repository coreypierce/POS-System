package root.api.table;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.SessionToken;
import edu.wit.se16.model.Shift;
import edu.wit.se16.model.Table;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.model.Table.TableStatus;
import edu.wit.se16.model.layout.LayoutJsonParams;
import edu.wit.se16.model.layout.Section;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestStatusUpdate implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer id = request.getBody("id", Integer::parseInt, null);
		TableStatus status = request.getBody("status", TableStatus::valueOf, null);
		
		// validate parameters
		if(id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing table-ID!");
		}
		
		if(status == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing or Bad status value!");
		}
		
		SessionToken token = SessionManager.getSessionToken();
		Employee employee = token.getEmployee();
		Table table;
		
		LOG.trace("Status update for Table #{} to '{}' requested by Employee #{}...", id, status, employee.getId());
		
		try {
			// attempt to load the table
			table = new Table(id);
			table.setStatus(status, employee);

		} catch(NoSuchElementException e) {
			LOG.warn("Could not find Table #{}, for statuc-update!", id);
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Could not find table!");
		}
		
		// Layout conversion Parameters
		LayoutJsonParams params = new LayoutJsonParams();
		Shift shift = Shift.getCurrentShift();
		
		// if the employee is a Server, then get their active section
		if(employee.getRole() == Role.Server && shift != null) {
			params.section = Section.findSection(shift, employee);
		}
		
		// send updated table back to client
		JsonBuilder.create()
			.append("success", true)
			.append("table", table.toJSON(params))
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "status"; }
}
