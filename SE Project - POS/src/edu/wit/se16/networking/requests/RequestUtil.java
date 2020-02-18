package edu.wit.se16.networking.requests;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import edu.wit.se16.networking.requests.RequestInfo.MethodType;

public class RequestUtil {
	public static IRequest redirect(String path, String redirect) {
		return new RedirectRequest(path, redirect);
	}
	
	public static <T extends IRequest> IRequest mirror(String extention, T handle, MethodType... types) {
		return new MirrorRequest<IRequest>(extention, handle, types);
	}
	
	// =============================== Redirect Request =============================== \\
	
	private static class RedirectRequest implements IRequest {
		private String path;
		private String redirect;
		
		public RedirectRequest(String path, String redirect) {
			this.path = path;
			this.redirect = redirect;
		}

		public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
			response.sendRedirect(redirect);
			return response;
		}

		public String getCommand() { return path; }
	}
	
	// =============================== Mirror Request =============================== \\
	
	private static class MirrorRequest<T extends IRequest> implements IRequest {
		private String extention;
		private T handle;
		
		private MethodType[] types;
		
		public MirrorRequest(String extention, T handle, MethodType... types) {
			this.extention = extention;
			this.handle = handle;
			
			// use handler's methods if none provided
			this.types = types == null || types.length == 0 ? handle.getMethod() : types;
		}
		
		public String getCommand() { return extention; }
		public MethodType[] getMethod() { return types; }

		public HttpServletResponse process(RequestInfo info, HttpServletResponse response) throws IOException, ServletException { 
			return handle.process(info, response); 
		}
	}

	// =============================== Blank Servlet Config =============================== \\
	
	public static class DefaultServletConfig implements ServletConfig {
		public String getServletName() { return null; }
		public ServletContext getServletContext() { return null; }
		public String getInitParameter(String name) { return null; }
		public Enumeration<String> getInitParameterNames() { return null; }
	}

//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private RequestUtil() { }
}
