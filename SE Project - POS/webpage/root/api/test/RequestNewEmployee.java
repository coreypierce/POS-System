package root.api.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;

public class RequestNewEmployee implements IRequest {

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		new Employee(request.getBody("first"), request.getBody("last"), Role.valueOf(request.getBody("role")));
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "new_employee"; }
}
