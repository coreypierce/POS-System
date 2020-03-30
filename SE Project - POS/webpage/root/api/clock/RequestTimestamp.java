package root.api.clock;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestTimestamp implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		
		Employee employee = SessionManager.getSessionToken().getEmployee();
		LOG.trace("Clock In/Out timestamp requested for Employee #{}...", employee.getId());
		
		employee.recordTimestamp();
		
		// respond OK
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "record"; }
}