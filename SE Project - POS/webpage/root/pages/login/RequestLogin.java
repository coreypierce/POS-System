package root.pages.login;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16._example.ExamplePassword;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestLogin implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		// gets username and password from request
		String username = request.getBody("username");
		String password = request.getBody("password");
		
		// for more info on available values, enable "trace" logs in the console and look at the request
		
		// always good to log activity, note the {} and the resulting message
		LOG.debug("User \"{}\" is trying to login...", username);
		
		// call login-handler 
		boolean isValid = ExamplePassword.validatePassword(username, password);
		
		// more logging
		if(isValid) {
			LOG.debug("\"{}\" has logged in", username);
		} else {
			LOG.warn("Invalid login for \"{}\"", username);
		}
		
		// respond with a JSON-Object, easy to parse in JS code
		JsonBuilder.create()
			// add result value
			.append("success", isValid)	
			// build Object and send to response
			.build(response);
		
		// set response code to OK - not strictly necessary but good practice 
		// Note: even if the login failed we still send "200 OK" as the JS code will process the result
		//		 we could have send "403 FORBIDDEN" if login failed, but then we would need to handle that on the JS side
		response.setStatus(HttpServletResponse.SC_OK);
		
		// always just return "response" 
		return response;
	}

	public String getCommand() { return "try_login"; }
}
