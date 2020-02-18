package edu.wit.se16.networking.requests;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import edu.wit.se16.networking.requests.RequestInfo.MethodType;

public interface IRequest {
	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException;
	
	public String getCommand();
	public default MethodType[] getMethod() { return null; }
}
