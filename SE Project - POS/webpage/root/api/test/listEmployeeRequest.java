package root.api.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class listEmployeeRequest implements IRequest {
	@Override
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) {
		Employee.listEmployees();
		return null;
	}

	@Override
	public String getCommand() {
		
		return "list_employees";
	}
	
}
