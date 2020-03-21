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
	
	// ThreadLocal as multiple connections (threads) could be running at once
	private static ThreadLocal<SessionToken> token = new ThreadLocal<>();
	private static ThreadLocal<CaseInsensitiveMap> sessionValues = new ThreadLocal<>();
	
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
		
		token.set(SessionToken.getToken(request));
		if(token.get() == null) return false;
		
		LOG.trace("Resuming session for Employee #{}", token.get().getEmployeeNumber());
		Employee employee = token.get().getEmployee();
		
		CaseInsensitiveMap sessionValues = new CaseInsensitiveMap();
		SessionManager.sessionValues.set(sessionValues);

		sessionValues.put("employee_firstname", employee.getFirstName());
		sessionValues.put("employee_lastname", employee.getLastName());
		sessionValues.put("employee_role", employee.getRole().toString().toLowerCase());
		sessionValues.put("employee_id", employee.getId());
		
		// check if the employee is active and hasn't been deleted
		boolean employee_usable = employee.isActive() && !employee.isDeleted();
		sessionValues.put("employee_usable", employee_usable);

		// employee is not usable; attempt start-session with set-token
		if(!employee_usable) return false;
		
		// if the client requested a "Non-Secure-Session" proceed without checking for temp-password
		String secure_sesson = request.getHeader("Non-Secure-Session");
		if(secure_sesson != null && secure_sesson.equalsIgnoreCase("true")) {
			return true;
		}

		// employee is not secure; attempt start-session with set-token
		if(employee.isTempPassword()) return false;
		
		return true;
	}
	
	public static void setSession(SessionToken token) {
		LOG.trace("Session started for Employee #{}", token.getEmployeeNumber());
		SessionManager.token.set(token);
	}
	
	public static void startSession(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		if(token.get() == null) {
			// Check if we should run the setup process
			if(SetupHandler.shouldEnterSetup()) {
				LOG.debug("No session found; Running setup process...");

				CaseInsensitiveMap sessionValues = new CaseInsensitiveMap();
				SessionManager.sessionValues.set(sessionValues);
				
				SetupHandler.runSetup(request, response);

			} else {
				LOG.debug("No session found; attempting to start new Session...");
		
				CaseInsensitiveMap sessionValues = new CaseInsensitiveMap();
				SessionManager.sessionValues.set(sessionValues);
				
				CaseInsensitiveMap values = new CaseInsensitiveMap();
				values.put("redirect_url", request.getRequest().getPathInfo());
				
				// open stream to HTML file, and open response stream
				InputStream in = HTMLResourceLoader.loadHTMLStream("root/pages/login/login.html", values);
				RequestPage.sendPage("root/pages/login/login.html", "login-page", in, request, response);
			}

		} else {
			LOG.debug("Insecure session found; directing employee to security-page...");
			
			// clone the current session-values
			CaseInsensitiveMap values = new CaseInsensitiveMap(SessionManager.sessionValues.get());
			values.put("redirect_url", request.getRequest().getPathInfo());
			
			// open stream to HTML file, and open response stream
			InputStream in = HTMLResourceLoader.loadHTMLStream("root/pages/login/secure/secure.html", values);
			RequestPage.sendPage("root/pages/login/secure/secure.html", "secure-page", in, request, response);
		}
	}
	
	/**
	 * 	Called right before returning to Client, used to finalize Session state
	 */
	public static void suspend(HttpServletResponse response) {
		if(token.get() != null) {
			LOG.trace("Suspending session for Employee #{}", token.get().getEmployeeNumber());
			token.get().appendSessionToken(response);
		}
	}
	
	public static SessionToken getSessionToken() { return token.get(); }
	public static CaseInsensitiveMap getSessionHTMLValues() { return sessionValues.get(); }
	
//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private SessionManager() {}
}
