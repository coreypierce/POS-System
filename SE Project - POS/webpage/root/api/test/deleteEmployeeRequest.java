package root.api.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class deleteEmployeeRequest implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();
	@Override
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			int employeeID = Integer.parseInt(request.getBody("id"));
			Employee.removeEmployee(employeeID);
		}catch(Exception e) {
			return StandardResponses.error(request, response, 400, "Error: Employee ID Error.");
		}
		return null;
	}

	@Override
	public String getCommand() {
		return "delete_user";
	}

}
