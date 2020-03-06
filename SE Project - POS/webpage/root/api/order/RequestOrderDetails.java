package root.api.order;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Order;
import edu.wit.se16.model.Table;
import edu.wit.se16.model.Table.TableStatus;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestOrderDetails implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer table_id = request.getBody("table_id", Integer::parseInt, null);
		
		// validate parameters
		if(table_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing 'table_id'");
		}
		
		// lookup table
		Table table = new Table(table_id);
		// validate table is in a reasonable state
		if(table.getStatus() == TableStatus.Open) {
			LOG.error("Requested Order for Table #{}, but table is currently open!", table_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Table shouldn't currently have an order");
		}
		
		// get the last order for the table
		Order order = Order.getTablesOrder(table_id);
		// if there never was on order on the specified table
		if(order == null) {
			LOG.error("Requested Order for Table #{}, but none exists!", table_id);
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Table doesn't currently have an order");
		}
		
		// respond with Order-JSON
		JsonBuilder.from(order.toJSON()).build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "details"; }
}
