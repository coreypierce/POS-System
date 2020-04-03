package root.api.shifts;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.model.Shift;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestShiftDetails implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Employee manager = SessionManager.getSessionToken().getEmployee();
		LOG.trace("Employee #{} is requesting to shift details...", manager.getId());
		
		// validate user is a Manager
		if(manager.getRole() != Role.Manager) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_FORBIDDEN, "Only Managers can access employee info!");
		}
		
		// lookup the current Shift
		Shift shift = Shift.getCurrentShift();
		
		if(shift != null) {
			JsonBuilder.create()
				.append("id", shift.getId())
				.append("type", shift.getShiftType().toString())
				.append("start_time", shift.getStartTime())
				.append("manager", shift.getManager().toJSON())
			.build(response);
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "details"; }
}