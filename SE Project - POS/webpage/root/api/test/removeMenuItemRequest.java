package root.api.test;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;

public class removeMenuItemRequest implements IRequest{
	@Override
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response)
			throws IOException, ServletException {
		int itemID = Integer.parseInt(request.getBody("id"));
		Menu.removeItemFromMenu(itemID);
		return null;
	}

	@Override
	public String getCommand() {
		return "delete_item";
	}

}
