package root.api.order;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Order;
import edu.wit.se16.model.Shift;
import edu.wit.se16.model.Table;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.model.Table.TableStatus;
import edu.wit.se16.model.layout.LayoutJsonParams;
import edu.wit.se16.model.layout.Section;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestNewOrder implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer table_id = request.getBody("table_id", Integer::parseInt, null);
		
		// validate parameters
		if(table_id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing 'table_id'");
		}
		
		// get the current employee and specified table
		Employee employee = SessionManager.getSessionToken().getEmployee();
		Table table = new Table(table_id);
		
		// Layout conversion Parameters
		LayoutJsonParams params = new LayoutJsonParams();
		Shift shift = Shift.getCurrentShift();
		
		// if the employee is a Server, then get their active section
		if(employee.getRole() == Role.Server && shift != null) {
			params.section = Section.findSection(shift, employee);
		}
		
		// if an order should already exist
		if(table.getStatus().ordinal() >= TableStatus.Order_Placed.ordinal()) {
			LOG.trace("Requested Order for Table #{}; order should already exists!", table_id);

			// get the last order for the table
			Order order = Order.getTablesOrder(table_id);
			if(order == null) {
				LOG.warn("Table #{} is in an invalid state! <Missing order>", table_id);
				
			} else {
				// respond with Order-JSON
				JsonBuilder.create()
					.append("order", order.toJSON())
					.append("table", table.toJSON(params))
				.build(response);
				response.setStatus(HttpServletResponse.SC_OK);
				return response;
			}
		}

		LOG.trace("Employee #{} is creating a new Order for Table #{}...", employee.getId(), table_id);
		
		// create a new Order for the table
		Order order = table.startOrder(employee);
		
		// respond with Order-JSON
		JsonBuilder.create()
			.append("order", order.toJSON())
			.append("table", table.toJSON(params))
		.build(response);
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "new"; }
}
