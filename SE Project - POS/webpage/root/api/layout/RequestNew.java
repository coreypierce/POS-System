package root.api.layout;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Table;
import edu.wit.se16.model.layout.LayoutJsonParams;
import edu.wit.se16.model.layout.RestaurantLayout;
import edu.wit.se16.model.layout.RestaurantLayout.Item;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestNew implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer pos_x = request.getBody("x", Integer::parseInt, null);
		Integer pos_y = request.getBody("y", Integer::parseInt, null);
		Integer width = request.getBody("width", Integer::parseInt, null);
		Integer height = request.getBody("height", Integer::parseInt, null);
		Integer rotation = request.getBody("rotation", Integer::parseInt, null);
		
		String tableDescript = request.getBody("table");
		
		// validate bound parameters
		if(pos_x == null || pos_y == null || width == null || height == null || rotation == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing position or bounds parameters!");
		}
		
		LOG.trace("Create a new {} at Position({}, {}) Bounds({}, {})", 
				tableDescript == null ? "Wall" : "Table", pos_x, pos_y, width, height);
		
		Item item = null;
		Table table = null;
		
		// if a table is requested
		if(tableDescript != null && !tableDescript.isEmpty()) {
			// create table
			table = new Table(tableDescript, 0);
		}

		// create item
		item = RestaurantLayout.getLayout()
				.newItem(rotation, new Point(pos_x, pos_y), new Dimension(width, height), table);
		
		// Layout conversion Parameters
		LayoutJsonParams params = new LayoutJsonParams();
		
		// send item back to requester 
		JsonBuilder.from(item.toJSON(params)).build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "new"; }
}
