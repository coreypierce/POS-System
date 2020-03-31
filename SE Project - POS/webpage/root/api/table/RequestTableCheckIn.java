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

public class RequestTableCheckIn implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer id = request.getBody("table_id", Integer::parseInt, null);
		
		// validate parameters
		if(id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing table-ID!");
		}
		
		SessionToken token = SessionManager.getSessionToken();
		Employee employee = token.getEmployee();
		Table table;
		
		LOG.trace("Employee #{} requested check-in on Table #{}...", employee.getId(), id);
		
		try {
			// attempt to load the table
			table = new Table(id);
			table.recordCheckIn(employee);

		} catch(NoSuchElementException e) {
			LOG.warn("Could not find Table #{}, for check-in!", id);
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Could not find table!");
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "check_in"; }
}
