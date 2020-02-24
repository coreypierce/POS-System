package root.api.login;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.security.PasswordUtil;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestLoginDetails implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer employee_id = request.getBody("employee_id", Integer::parseInt, null);
		LOG.trace("Login-Details requested for Employee #{}", employee_id);
		
		if(employee_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "No employee-id provided!");
		}
		
		String salt;
		try {
			LOG.trace("Loading requested employee...");
			Employee employee = new Employee(employee_id);
			salt = employee.getSalt();

			LOG.trace("Employe found, sending login-details...");
			
		} catch(NoSuchElementException e) {
			LOG.warn("No Employee #{}, generating fake-details...", employee_id);
			salt = PasswordUtil.generateFakeSalt(employee_id);
		}
		
		JsonBuilder.create()
			.append("salt", salt)
			.append("keysize", PasswordUtil.KEY_SIZE)
			.append("iterations", PasswordUtil.ITERATIONS)
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "details"; }
}