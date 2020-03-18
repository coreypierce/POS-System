package root.api.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;

public class addMenuItemRequest implements IRequest{
	@Override
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response)
			throws IOException, ServletException {
		String itemName = request.getBody("item");
		double price = Double.parseDouble(request.getBody("price"));
		Menu.addItemToMenu(itemName, price);
		return null;
	}

	@Override
	public String getCommand() {
		return "add_item";
	}

}
