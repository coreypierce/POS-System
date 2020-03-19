package edu.wit.se16.model;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.security.PasswordUtil;
import edu.wit.se16.system.logging.LoggingUtil;

public class SessionToken {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final String COOKIE_NAME = "Session-Token"; // TODO: reduce to 5 minute-timeout
	private static final Duration LOGIN_TIMEOUT = Duration.ofMinutes(5000);
	
	private static final PreparedStatement QUERY = Database.prep(
			"SELECT * FROM session_tokens WHERE id = ? ORDER BY expiration DESC LIMIT 1");
	
	private static final PreparedStatement INSERT = Database.prep(
			"INSERT INTO session_tokens (id, employee_id, expiration) VALUES(?, ?, ?)");
	
	private static final PreparedStatement UPDATE_EXPERATION = Database.prep(
			"UPDATE session_tokens SET expiration = ? WHERE id = ?");

	private static final PreparedStatement DELETE_TOKEN = Database.prep("DELETE FROM session_tokens WHERE employee_id = ?");

	private Cookie cookie;

	private String token;
	private int employee_id;
	private Instant experation;
	
	private SessionToken(Cookie cookie) {
		this(cookie, false);
	}
	
	private SessionToken(Cookie cookie, boolean isNew) {
		this.cookie = cookie;
		this.token = cookie.getValue();
		
		if(!isNew) {
			boolean exists = Database.query(results -> {
				this.employee_id = results.getInt("employee_id");
				this.experation = results.getTimestamp("expiration").toInstant();
				
			}, QUERY, this.token);
			
			if(!exists) {
				// if we're unable to find the token
				throw new NoSuchElementException();
			}
		} else {
			updateExpiration();
		}
	}
	
	/**
	 * 	Takes in a Request from the client and looks for a SessionToken-Cookie. <br/>
	 * 
	 * 	If this cookie is found, and is valid, then a SessionToken is returned and the expiration updated.
	 * 	Otherwise, null returned.
	 */
	public static SessionToken getToken(RequestInfo request) {
		HttpServletRequest http_request = request.getRequest();
		Cookie[] cookies = http_request.getCookies();
		SessionToken token = null;
		
		if(cookies != null) { 
			for(Cookie cookie : cookies) {
				if(cookie.getName().equals(COOKIE_NAME)) {
					try {
						// find the last usable cookie
						token = new SessionToken(cookie);
					} catch(NoSuchElementException e) {
						// if a token was provided, but doesn't exist
					}
				}
			}
		}
		
		// check if the token is still valid
		if(token != null && !token.hasExpired()) {
			// if the token is valid, update the expiration data
			token.updateExpiration();
			
		} else {
			token = null;
		}
		
		return token;
	}
	
	/**
	 * 	Generates a new SessionToken for the provided Employee
	 */
	public static SessionToken generateToken(Employee employee) {
		String sessionToken = PasswordUtil.generateSessionToken();
		Cookie cookie = new Cookie(COOKIE_NAME, sessionToken);
		SessionToken token = new SessionToken(cookie, true);

		token.employee_id = employee.getId();
		// set cookie to be used on all pages on the site
		cookie.setPath("/");

		// remove all old session-tokens
		Database.update(DELETE_TOKEN, employee.getId());
		
		// inserts the token into the database
		Database.update(INSERT, sessionToken, employee.getId(), Timestamp.from(token.experation));
		return token;
	}
	
// =========================================== End Session =========================================== \\
	
	public void endSession() {
		LOG.trace("Ending Employee #{} session...", this.employee_id);
		Database.update(DELETE_TOKEN, employee_id);
	}
	
	public static void clearEmplyeesSession(Employee employee) {
		LOG.trace("Manualy ending Session for Employee #{}...", employee.getId());
		Database.update(DELETE_TOKEN, employee.getId());
	}

// =========================================== Expiration =========================================== \\
	
	public boolean hasExpired() { 
		boolean expired = this.experation.isBefore(Instant.now());
		// if the session has expired, delete the token
		if(expired) endSession();
		return expired;
	}
	
	public void updateExpiration() {
		LOG.trace("Updating Session-Token ({}) for Employe #{}...", this.token, this.employee_id);
		
		// calculate the new expiration time
		this.experation = Instant.now().plus(LOGIN_TIMEOUT);
		LOG.trace("Token ({}) now Expiers at: {}", this.token, this.experation);
		
		// update the database with the new expiration-date
		Database.update(UPDATE_EXPERATION, Timestamp.from(experation), this.token);
	}

// =========================================== Response =========================================== \\
	
	public void appendSessionToken(HttpServletResponse response) {
		response.addCookie(this.cookie);
	}
	
	public int getEmployeeNumber() { return employee_id; }
	public Employee getEmployee() { return new Employee(employee_id); }
}
