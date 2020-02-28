package edu.wit.se16.networking;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;

import edu.wit.se16.networking.requests.DefaultFileRequest;
import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.networking.requests.RequestUtil;
import edu.wit.se16.system.logging.LoggingUtil;
import root.Automapper;

public class RequestDistributor extends AbstractHandler {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private Map<String, IRequest> requests;
	private IRequest defaultFileHandler;
	
	public RequestDistributor() {
		this.requests = new HashMap<>();
		
		// ================================ Setup Default Resource Access ================================ \\
		
			String rootPackage = Automapper.class.getPackage().getName().replace('.', '/');
			this.defaultFileHandler = new DefaultFileRequest(rootPackage);
			
		// ================================ Setup File Paths ================================ \\

			redgesterRequestHandler(Automapper.loadPackage());
	
			//redgesterRequestHandler("/", RequestUtil.redirect("", "/index"));
			redgesterRequestHandler("/", RequestUtil.mirror("", requests.get("/index")));
	}
		
	public void redgesterRequestHandler(Map<String, IRequest> requests) { 
		this.requests.putAll(requests);
	}
	
	public void redgesterRequestHandler(String path, IRequest request) { 
		// Make sure path is prefixed with /
		path = StringUtils.prependIfMissing("/", path);
		// if path only consists of a /, clear it to avoid //
		if(path.length() <= 1) path = "";
		
		requests.put(path + "/" + request.getCommand(), request); 
	}
	
	public void setFileHandler(IRequest handler) { this.defaultFileHandler = handler; }
	
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		LOG.debug("Request \"{}\" received from {}:{}", target, request.getRemoteHost(), request.getRemotePort());
		
		try {
			RequestInfo requestInfo = new RequestInfo(request);
			// Mark that the request has been received and processed
			baseRequest.setHandled(true);
			
			// only attempt session load on request, default resources don't need session
			if(requests.containsKey(target)) {
				// attempt to resume session, if not start new session
				if(!SessionManager.resume(requestInfo)) {
					SessionManager.startSession(requestInfo, response);
					return;
				}
				
				// check for a request-handler mapped to the target address
				if(tryRequest(target, requestInfo, response)) 
					return;
			}
			
			// Check if request is for a file
			if(defaultFileHandler != null && requestInfo.getFileName().contains(".")) {
				defaultFileHandler.process(requestInfo, response);
				return;
			}
	
			// if no resource was found at the requested address, return 404
			LOG.warn("No such resource/command found: {}", target);
			StandardResponses.missing(requestInfo, response, target);
		
		} catch(IllegalArgumentException e) {
			// Catch RequestInfo parsing exceptions
			StandardResponses.error(null, response, HttpServletResponse.SC_BAD_REQUEST, "Request method is not supported");
			
		} finally {
			SessionManager.suspend(response);
		}
	}
	
	/**
	 * 	Attempts to find a {@link IRequest Request-Handler} that is mapped to the provided address
	 * 
	 * 	@return
	 * 		true if a handler is found, false otherwise
	 */
	private boolean tryRequest(String target, RequestInfo requestInfo, HttpServletResponse response) throws IOException {
		// check for a request-handler that is mapped to the given address
		IRequest handle = requests.get(target);
		if(handle == null) return false;
		
		// If a handler is found for the provided address
		try {
			// Offer the Request to the selected Handler 
			handle.process(requestInfo, response);
			
			// Log warnings to Logs
			if(requestInfo.getWarnings().length != 0) {
				LOG.debug("Request \"{}\" completed with the following warnings: {}", target, Arrays.toString(requestInfo.getWarnings()));
			}
			
			// return request-handled
			return true;
			
		} catch(Exception e) {
			LOG.error("An Unexpected error occured while processing the request: " + target, e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An Unexpected Error has occurred while processing you request!");
			return true; // Error Counts as Handled
			
//			Would rather use this, but that would involve flushing data to the Stream
//			StandardResponses.error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An Unexpected Error has occurred while processing you request!");
		}
	}
}
