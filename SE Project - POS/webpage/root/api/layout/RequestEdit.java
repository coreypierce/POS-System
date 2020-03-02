package root.api.layout;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.layout.RestaurantLayout;
import edu.wit.se16.model.layout.RestaurantLayout.Item;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestEdit implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer id = request.getBody("id", Integer::parseInt, null);
		
		Integer pos_x = request.getBody("x", Integer::parseInt, null);
		Integer pos_y = request.getBody("y", Integer::parseInt, null);
		Integer width = request.getBody("width", Integer::parseInt, null);
		Integer height = request.getBody("height", Integer::parseInt, null);
		Integer rotation = request.getBody("rotation", Integer::parseInt, null);
		
		// validate parameters
		if(id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing item-ID!");
		}
		
		if(pos_x == null || pos_y == null || width == null || height == null || rotation == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing position or bounds parameters!");
		}
		
		LOG.trace("Editting layout-item #{}...", id);
		JsonBuilder builder = JsonBuilder.create();
		
		try {
			Item item = RestaurantLayout.getLayout().getItem(id);

			// attempt item update
			if(item.updateConstraints(rotation, new Point(pos_x, pos_y), new Dimension(width, height))) {
				builder.append("status", "success");
				
			} else {
				LOG.trace("Failed to update bounds of layout-item #{}", id);
				builder
					.append("status", "failed")
					.append("error", "DML error occurred");
			}
		
		// If the item doesn't exist
		} catch(NoSuchElementException e) {
			LOG.warn("Could not find layout-item #{}!", id);
			
			builder
				.append("status", "failed")
				.append("error", "item with provided id not found");
		}
		
		builder.build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "edit"; }
}
