package root.api.host;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.model.Shift;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestSectionRecommendation implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer guest_count = request.getBody("guest_count", Integer::parseInt, null);
		
		// validate parameters
		if(guest_count == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing 'guest_count'");
		}
		
		Shift shift = Shift.getCurrentShift();
		if(shift == null) {
			LOG.error("No shift found in the system!");
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_CONFLICT, "There is no active shift available");
		}
		
		LOG.trace("Requesting section recomendation for {} guests durring Shift #{}...", guest_count, shift.getId());

		AtomicInteger minCount = new AtomicInteger(Integer.MAX_VALUE);
		AtomicInteger minCount_sectionId = new AtomicInteger(0);
		
		Database.query(results -> {
			int count = results.getInt("count");
			
			if(count < minCount.get()) {
				minCount.set(results.getInt("count"));
				minCount_sectionId.set(results.getInt("section"));
			}
		}, RequestSectionSeatCounts.QUERY, shift.getId());
		
		if(minCount_sectionId.get() > 0) {
			JsonBuilder.create()
				.append("section_number", minCount_sectionId.get())
			.build(response);
			
			response.setStatus(HttpServletResponse.SC_OK);
		
		} else {
			response.setStatus(HttpServletResponse.SC_NO_CONTENT);
		}
		
		return response;
	}

	public String getCommand() { return "recommend"; }
}