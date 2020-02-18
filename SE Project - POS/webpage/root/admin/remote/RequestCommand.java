package root.admin.remote;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.system.logging.console.LogConsoleStreamProcessor;

public class RequestCommand implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		String command = request.getBody("exe");
		
		if(command == null) {
			LOG.trace("Remote command recieved: {}", "<missing>");
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "missing paramater \"exe\"");
		}
		
		LOG.trace("Remote command recieved: {}", command);
		LogConsoleStreamProcessor.sendInput(command);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "command"; }
}
