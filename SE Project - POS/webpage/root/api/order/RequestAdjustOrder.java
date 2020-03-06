package root.api.order;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Order;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestAdjustOrder implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		@SuppressWarnings("unchecked")
		ArrayList<Map<String, String>> items = (ArrayList<Map<String, String>>) request.getBodyRaw("items");
		Integer order_id = request.getBody("id", Integer::parseInt, null);
 		
		// validate parameters
		if(order_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing order 'id'");
		}
		
		if(items == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing 'items'");
		}

		try {
			// lookup order
			Order order = new Order(order_id);
			order.update(items);
			
		} catch(NoSuchElementException e) {
			LOG.error("Requested Order #{} doesn't none exists!", order_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Specified Order does not exist!");
			
		} catch(NullPointerException | NumberFormatException e) {
			LOG.error("Order-Update request contains malfored item-data: {}", e.getMessage());
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Malformed data in 'items'!");
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "edit"; }
}
