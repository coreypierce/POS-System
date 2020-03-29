package root.api.layout.section;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.layout.Section;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestDeleteTable implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer section_id = request.getBody("section_id", Integer::parseInt, null);
		
		// validate inputs
		if(section_id == null) {
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Missing or invalid 'section_id'");
		}
		
		LOG.trace("Requesting to remove Section #{}...", section_id);

		Section section;
		
		try {
			section = new Section(section_id);
			section.delete();
					
		} catch(NoSuchElementException e) {
			LOG.warn("no such section Section #{}!", section_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Could not find the specified Section!");
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "delete"; }
}