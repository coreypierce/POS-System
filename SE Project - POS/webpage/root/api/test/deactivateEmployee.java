package root.api.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;

public class deactivateEmployee implements IRequest{
	@Override
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response)
			throws IOException, ServletException {
		int employeeID = Integer.parseInt(request.getBody("id"));
		Employee.deactivateEmployee(employeeID);
		return null;
	}

	@Override
	public String getCommand() {
		return "deact_user";
	}
	
}
