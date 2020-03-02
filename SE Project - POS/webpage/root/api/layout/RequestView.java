package root.api.layout;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.model.Shift;
import edu.wit.se16.model.layout.LayoutJsonParams;
import edu.wit.se16.model.layout.RestaurantLayout;
import edu.wit.se16.model.layout.Section;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestView implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		LOG.trace("Access restaurant-layout...");
		RestaurantLayout layout = RestaurantLayout.getLayout();

		// get the current employee
		Employee employee = SessionManager.getSessionToken().getEmployee();
		Shift shift = Shift.getCurrentShift();
		
		// Layout conversion Parameters
		LayoutJsonParams params = new LayoutJsonParams();
		
		// if the employee is a Server, then get their active section
		if(employee.getRole() == Role.Server && shift != null) {
			params.section = Section.findSection(shift, employee);
		}
		
		// convert Restaurant-Layout to JSON and send it back to client
		JsonBuilder
			.from(layout.toJSON(params))
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "get_layout"; }
}
