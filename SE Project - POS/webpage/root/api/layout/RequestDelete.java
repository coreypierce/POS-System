package root.api.layout;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.layout.RestaurantLayout;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestDelete implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer id = request.getBody("id", Integer::parseInt, null);
		
		// validate parameters
		if(id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing item-ID!");
		}
		
		LOG.trace("Attempting to delete restaurant-item #{}...", id);
		RestaurantLayout.getLayout().deleteItem(id);
		
		// send item back to requester 
		JsonBuilder.create()
			.append("success", true)
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "delete"; }
}
