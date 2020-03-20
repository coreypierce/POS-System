package root.api.login;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestLogout implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		LOG.trace("Logout request for Employee #{}...", SessionManager.getSessionToken().getEmployeeNumber());
		SessionManager.getSessionToken().endSession();

		response.setStatus(HttpServletResponse.SC_OK);
		response.sendRedirect("/");
		return response;
	}

	public String getCommand() { return "logout"; }
}