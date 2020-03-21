package root.api.login;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.SessionToken;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestSetPassword implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		String password = request.getBody("password");
		
		// validate parameters
		
		if(password == null || password.isEmpty()) {
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Invalid password!");
		}
		
		Employee employee = SessionManager.getSessionToken().getEmployee();
		LOG.trace("Employee #{} has submitted a new permanent password...", employee.getId());
		
		// assign new password, destroys session token
		employee.setPassword(password);
		
		// generate new login token
		SessionToken token = SessionToken.generateToken(employee);
		SessionManager.setSession(token);

		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "set_password"; }
}