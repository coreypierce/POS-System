package root.api.layout.section;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Shift;
import edu.wit.se16.model.layout.Section;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestNewSection implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Shift current_shift = Shift.getCurrentShift();
		
		if(current_shift == null) {
			LOG.error("No shift found in the system!");
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "There is no active shift available");
		}
		
		LOG.trace("New Section has been requested durring Shift #{}...", current_shift.getId());
		
		// create new shift
		Section section = new Section(current_shift);
		
		JsonBuilder.from(section.toJSON()).build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "new"; }
}