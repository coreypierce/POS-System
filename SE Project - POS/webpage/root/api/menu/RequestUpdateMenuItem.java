package root.api.menu;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.menu.MenuCategory;
import edu.wit.se16.model.menu.MenuItem;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestUpdateMenuItem implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer id = request.getBody("id", Integer::parseInt, null);
		
		String name = request.getBody("name");
		String category_name = request.getBody("category");
		Double price = request.getBody("price", Double::parseDouble, null);
		
		// validate parameters
		if(id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing parameter 'id'");
		}
		
		if(name == null || category_name == null || price == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: name, category, or price");
		}
		
		LOG.trace("Requesting update to Menu-Item #{}; '{}' for ${} in the category '{}'...", id, name, price, category_name);
		MenuItem item;
		
		try {
			item = new MenuItem(id);
			
			MenuCategory category = new MenuCategory(category_name);
			item.update(category.getId(), name, price);
			
		} catch(NoSuchElementException e) {
			LOG.error("Requested Menu-Item #{}, but none exists!", id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Menu-Item doesn't exist");
		}
		
		JsonBuilder.from(item.toJSON())
			// append name, just in case category is new
			.append("category", category_name)
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "update"; }
}
