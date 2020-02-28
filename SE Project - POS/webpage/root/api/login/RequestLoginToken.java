package root.api.login;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.SessionToken;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.security.PasswordUtil;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestLoginToken implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer employee_id = request.getBody("employee_id", Integer::parseInt, null);
		String password_hash = request.getBody("password");

		String salt = request.getBody("salt");
		Integer key_size = request.getBody("key_size", Integer::parseInt, null);
		Integer iterations = request.getBody("iterations", Integer::parseInt, null);
		
		LOG.trace("Login-Token requested for Employee #{}", employee_id);
		
		if(employee_id == null || password_hash == null || salt == null || key_size == null || iterations == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Invalid request; Missing key-parameter(s)");
		}
		
		try {
			LOG.trace("Attempting to load Employee #{}...", employee_id);
			Employee employee = new Employee(employee_id);

			LOG.trace("Checking provided password...");
			String check_hash = PasswordUtil.generateValidationHash(employee.getPassword(), salt, iterations, key_size);

			// check if provided password matches database
			if(check_hash.equals(password_hash)) {
				LOG.trace("Employee #{} login successful!", employee_id);
				
				SessionToken token = SessionToken.generateToken(employee);
				SessionManager.setSession(token);
				
				response.setStatus(HttpServletResponse.SC_OK);
				return response;
			}
			
			// if no match, fall-through to return error
		} catch(NoSuchElementException e) {
			LOG.warn("Invalid Employee #{} requested login!", employee_id);
		}

		LOG.warn("Failed login attempt for Employee #{}!", employee_id);
		return StandardResponses.error(request, response, 
				HttpServletResponse.SC_UNAUTHORIZED, "Invalid Login!");
	}

	public String getCommand() { return "token"; }
}
