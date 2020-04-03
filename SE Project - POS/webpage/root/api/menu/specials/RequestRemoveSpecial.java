package root.api.menu.specials;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Shift;
import edu.wit.se16.model.menu.MenuItem;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestRemoveSpecial implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer item_id = request.getBody("item_id", Integer::parseInt, null);
		
		// validate parameters
		if(item_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing menu-item 'item_id'");
		}
		
		MenuItem item;
		
		Shift shift = Shift.getCurrentShift();
		if(shift == null) {
			LOG.error("No shift found in the system!");
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_CONFLICT, "There is no active shift available");
		}
		
		LOG.trace("Removing special-price for menu-Item #{} from Shift #{}...", item_id, shift.getId());

		try {
			item = new MenuItem(item_id);
			item.removeSpecial(shift);
			
		} catch(NoSuchElementException e) {
			LOG.error("Requested Menu-Item #{}, but none exists!", item_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Menu-Item doesn't exist");
		}

		JsonBuilder.from(item.toJSON(shift)).build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "remove"; }
}
