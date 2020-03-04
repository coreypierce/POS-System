package edu.wit.se16.networking;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.slf4j.Logger;

import edu.wit.se16.model.Employee;
import edu.wit.se16.model.SessionToken;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.networking.requests.RequestPage;
import edu.wit.se16.networking.requests.loaders.HTMLResourceLoader;
import edu.wit.se16.system.logging.LoggingUtil;

public class SessionManager {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static SessionToken token;
	
	private static CaseInsensitiveMap sessionValues;
	
	/**
	 * 	Attempts to resume a Session. <br/>
	 * 	Returns false if session could not be resumed
	 */
	public static boolean resume(RequestInfo request) {
		// allows for a client to request that no-session be used
		String session_state = request.getHeader("Non-Session");
		if(session_state != null && session_state.equalsIgnoreCase("true")) {
			return true;
		}
		
		token = SessionToken.getToken(request);
		if(token == null) return false;
		
		LOG.trace("Resuming session for Employee #{}", token.getEmployeeNumber());
		
		Employee employee = token.getEmployee();
		sessionValues = new CaseInsensitiveMap();
		
		sessionValues.put("employee_firstname", employee.getFirstName());
		sessionValues.put("employee_lastname", employee.getLastName());
		sessionValues.put("employee_role", employee.getRole().toString().toLowerCase());
		
		return true;
	}
	
	public static void setSession(SessionToken token) {
		LOG.trace("Session started for Employee #{}", token.getEmployeeNumber());
		SessionManager.token = token;
	}
	
	public static void startSession(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		LOG.debug("No session found; attempting to start new Session...");
		sessionValues = new CaseInsensitiveMap();
		
		CaseInsensitiveMap values = new CaseInsensitiveMap();
		values.put("redirect_url", request.getRequest().getPathInfo());
		
		// open stream to HTML file, and open response stream
		InputStream in = HTMLResourceLoader.loadHTMLStream("root/pages/login/login.html", values);
		RequestPage.sendPage("root/pages/login/login.html", "login-page", in, request, response);
	}
	
	/**
	 * 	Called right before returning to Client, used to finalize Session state
	 */
	public static void suspend(HttpServletResponse response) {
		if(token != null) {
			LOG.trace("Suspending session for Employee #{}", token.getEmployeeNumber());
			token.setSession(response);
		}
	}
	
	public static SessionToken getSessionToken() { return token; }
	public static CaseInsensitiveMap getSessionHTMLValues() { return sessionValues; }
	
//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private SessionManager() {}
}
