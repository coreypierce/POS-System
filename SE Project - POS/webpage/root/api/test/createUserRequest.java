package root.api.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class createUserRequest implements IRequest {
private static final Logger LOG = LoggingUtil.getLogger();
	@Override
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response)
			throws IOException, ServletException {
			String firstName = request.getBody("First_Name");
			String lastName = request.getBody("Last_Name");
			if(firstName == null || lastName == null) {
				LOG.warn("First Name or Last Name were empty...");
				return StandardResponses.error(request, response, 400, "Error: NULL Value Found");
			}	
			Employee newEmployee = new Employee(firstName, lastName);
			
			response.setStatus(HttpServletResponse.SC_OK);
			LOG.trace("{} {} added...", firstName, lastName);
			
		return null;
	}

	@Override
	public String getCommand() {
		
		return "create_user";
	}
	
}
