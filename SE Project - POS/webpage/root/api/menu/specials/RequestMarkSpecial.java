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

public class RequestMarkSpecial implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer item_id = request.getBody("item_id", Integer::parseInt, null);
		Double price = request.getBody("price", Double::parseDouble, null);
		
		// validate parameters
		if(item_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing menu-item 'item_id'");
		}
		
		if(price == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing 'price'");
		}
		
		MenuItem item;
		Shift shift = Shift.getCurrentShift();
		
		LOG.trace("Set special price for Menu-Item #{} to ${} for Shift #{}...", item_id, price, shift.getId());

		try {
			item = new MenuItem(item_id);
			item.markSpecial(shift, price);
			
		} catch(NoSuchElementException e) {
			LOG.error("Requested Menu-Item #{}, but none exists!", item_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Menu-Item doesn't exist");
		}
		
		JsonBuilder.from(item.toJSON(shift)).build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "set"; }
}
