package root.api.table;

import java.io.IOException;
import java.util.NoSuchElementException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.Order;
import edu.wit.se16.model.Employee.Role;
import edu.wit.se16.model.SessionToken;
import edu.wit.se16.model.Shift;
import edu.wit.se16.model.Table;
import edu.wit.se16.model.layout.LayoutJsonParams;
import edu.wit.se16.model.layout.Section;
import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class RequestPrintCheck implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		Integer id = request.getBody("table_id", Integer::parseInt, null);
		
		// validate parameters
		if(id == null) {
			return StandardResponses.error(request, response, 
					HttpServletResponse.SC_BAD_REQUEST, "Missing table-ID!");
		}
		
		SessionToken token = SessionManager.getSessionToken();
		Employee employee = token.getEmployee();
		
		LOG.trace("Employee #{} requested the bill for Table #{}...", employee.getId(), id);

		Table table;
		double amount;
		
		Order order;
		
		try {
			// attempt to load the table
			table = new Table(id);
			amount = table.printCheck(employee);
			
			order = Order.getTablesOrder(id);

		} catch(NoSuchElementException e) {
			LOG.warn("Could not find Table #{}, to print-check for!", id);
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "Could not find table!");
			
		} catch (IllegalStateException e) {
			LOG.warn("Table #{} does not currently have an Order!", id);
			return StandardResponses.error(request, response, HttpServletResponse.SC_BAD_REQUEST, "No open Order for specified Table!");
		}
		
		// TODO: get stored tax-rate;
		double taxRate = .0625;
		// round-total to 2-decimal places
		double taxAmount = (double) Math.round((amount * taxRate) * 100) / 100;
		
		double total = taxAmount + amount;
		
		// Layout conversion Parameters
		LayoutJsonParams params = new LayoutJsonParams();
		Shift shift = Shift.getCurrentShift();
		
		// if the employee is a Server, then get their active section
		if(employee.getRole() == Role.Server && shift != null) {
			params.section = Section.findSection(shift, employee);
		}
		
		// send updated table back to client
		JsonBuilder.create()
			.append("success", true)
			.append("order_details", order.toJSON())
			
			.append("sub_total", amount)
			.append("tax_rate", taxRate * 100)
			.append("tax_ammount", taxAmount)
			.append("total", total)
			
			.append("table", table.toJSON(params))
		.build(response);
		
		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return "print_check"; }
}