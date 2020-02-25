package root.api.layout;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.layout.RestaurantLayout;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestView implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		LOG.trace("Access restaurant-layout...");
		RestaurantLayout layout = RestaurantLayout.getLayout();

		// convert Restaurant-Layout to JSON and send it back to client
		JsonBuilder
			.from(layout.toJSON())
//		JsonBuilder.create()
//			.append("width", 35)
//			.append("height", 25)
//			.newArray("items")
//				// Table 1
//				.newObject()
//					.newObject("position")
//						.append("x", 8)
//						.append("y", 3)
//					.end()
//					.newObject("bounds")
//						.append("width", 7)
//						.append("height", 4)
//					.end()
//					.newObject("table")
//						.append("id", 6)
//						.append("name", "TB-6")
//						.append("icon", "apple")
//					.end()
//				.end()
//
//				// Wall 3
//				.newObject()
//					.newObject("position")
//						.append("x", 23)
//						.append("y", 5)
//					.end()
//					.newObject("bounds")
//						.append("width", 1)
//						.append("height", 15)
//					.end()
//				.end()
//				// Wall 2
//				.newObject()
//					.newObject("position")
//						.append("x", 23)
//						.append("y", 19)
//					.end()
//					.newObject("bounds")
//						.append("width", 5)
//						.append("height", 1)
//					.end()
//				.end()
//				// Wall 1
//				.newObject()
//					.newObject("position")
//						.append("x", 27)
//						.append("y", 8)
//					.end()
//					.newObject("bounds")
//						.append("width", 5)
//						.append("height", 8)
//					.end()
//				.end()
//			.end()
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "get_layout"; }
}
