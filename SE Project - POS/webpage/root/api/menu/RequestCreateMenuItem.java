package root.api.menu;

import java.io.IOException;

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

public class RequestCreateMenuItem implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		String name = request.getBody("name");
		String category_name = request.getBody("category");
		Double price = request.getBody("price", Double::parseDouble, null);
		
		// validate parameters
		if(name == null || category_name == null || price == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing required parameter: name, category, or price");
		}
		
		LOG.trace("Requesting new Menu-Item({}) for ${} in the category '{}'...", name, price, category_name);

		MenuCategory category = new MenuCategory(category_name);
		MenuItem item = new MenuItem(name, price, category);
		
		JsonBuilder.from(item.toJSON()).build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "new"; }
}
