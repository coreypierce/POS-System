package root.api.layout.section;

import java.io.IOException;
import java.util.List;

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

public class RequestListSections implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Shift current_shift = Shift.getCurrentShift();
		
		if(current_shift == null) {
			LOG.error("No shift found in the system!");
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "There is no active shift available");
		}
		
		LOG.trace("Requested list of all Sections in Shift #{}...", current_shift.getId());
		List<Section> sections = current_shift.getAllSections();

		JsonBuilder builder = JsonBuilder.create()
				.newArray("sections");
		
		for(Section section : sections) {
			builder.append(section.toJSON());
		}
		
		builder
			.end()
			.build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "list"; }
}