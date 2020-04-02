package root.api.menu;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Shift;
import edu.wit.se16.model.menu.MenuCategory;
import edu.wit.se16.model.menu.MenuItem;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestMenuItems implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Map<Integer, String> categories = MenuCategory.getAllCategories();
		MenuItem[] items = MenuItem.getAllItems();

		Shift shift = Shift.getCurrentShift();

		LOG.trace("Found {} categories", categories.size());
		LOG.trace("Found {} menu-items", items.length);
		
		JsonBuilder builder = JsonBuilder.create();
		
		builder.newObject("categories");
		for(Entry<Integer, String> category : categories.entrySet()) {
			builder.append(category.getKey() + "", category.getValue());
		}
		
		builder.end();
		builder.newArray("items");
		
		// write all items into JSON
		for(MenuItem item : items) {
			builder.append(item.toJSON(shift));
		}
		
		// end array and send to client
		builder.end();
		
		builder.build(response);

		// respond OK
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "list"; }
}