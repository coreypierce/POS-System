package edu.wit.se16.networking.requests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;

import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonReader;

public class RequestInfo {
	private static final Logger LOG = LoggingUtil.getLogger();
	private static final String NONE = "";
	
	private MethodType type;
	private String file;
	
	private Map<String, Object> body;
	private HttpServletRequest request;
	
	private ArrayList<String> warnings;
	
	public RequestInfo(HttpServletRequest request) throws IOException {
		this.request = request;
		this.warnings = new ArrayList<>();
		
		
		// extract file name from URL-path
		String path = request.getPathInfo();
		if(path == null) path = "/";
		this.file = path.substring(path.lastIndexOf('/'));
		
		try { 
			// convert request-method into enum value
			this.type = MethodType.valueOf(request.getMethod()); 
			
		} catch(IllegalArgumentException e) { 
			// if using an unsupported request-method
			LOG.error("Unsupported Request-Type: {}", request.getMethod());
			throw e; 
		}
		
		// parse content body
		String contentType = request.getHeader("Content-Type");
		@SuppressWarnings("unused")
		String type = NONE, encoding = "UTF-8";
		
		if(contentType != null) {
			// separate mine-type and encoding information 
			String[] parts = contentType.split(";");
			type = parts[0].trim();
			
			if(parts.length > 1) 
				encoding = parts[1].trim();
		}
		
		// if request-method is GET, then data is always type "x-www-form-urlencoded"
		if(this.type == MethodType.GET) {
			type = "application/x-www-form-urlencoded";
		}
		
		// parse request-body, if known format
		switch(type) {
			case "application/x-www-form-urlencoded":
				body = new HashMap<>();
				
				for(Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
					String[] array = entry.getValue();
					String key = entry.getKey().toLowerCase();
					
					if(array.length == 0) body.put(key, NONE);
					else if(array.length == 1) body.put(key, array[0]);
					else body.put(key, array);
				}
			break;
				
			case "application/json":
				body = new HashMap<>();
				try { body = JsonReader.read(request.getInputStream()); }
				catch(JsonParseException e) { warnings.add("Json Parsing Error: " + e.getMessage().split("\\R")[0]); }
			break;
				
			case NONE: default:
				if(request.getInputStream().read() == -1) break;
				warnings.add("Unsupported body format: " + type);
			break;
		}
		
		LOG.trace("New Request Created: \n\tHeaders = {}\n\tBody = {}", getHeadersMap(), body);
	}
	
	/**	Get request-header by name */
	public String getHeader(String name) { return request.getHeader(name); }

	/**	Get request-body value by name */
	public String getBody(String name) { 
		Object o; return (o = body.get(name.toLowerCase())) == null ? null : String.valueOf(o); 
	}
	
	public Object getBodyRaw(String name) { 
		return body.get(name.toLowerCase());
	}

	/**	Get all mapped values from the request-body */
	public Map<String, Object> getBody() { return Collections.unmodifiableMap(body); }
	
	@SuppressWarnings("unchecked")
	/**
	 * 	Get a value from the request-body by name <br />
	 * 
	 * 	Value is run through {@code converter} function and returned. <br />
	 * 	If converter fails, or value is not found, then {@code defaultValue} is returned 
	 */
	public <R> R getBody(String name, Function<String, R> convert, R defaultValue) { 
		try {
			Object val = body.get(name.toLowerCase());
			if(val instanceof String) 
				return convert.apply((String) body.get(name.toLowerCase()));
			return (R) val;
		} catch(Exception e) { return defaultValue; }
	}
	
	public String getFileName() { return file; }
	public MethodType getMethod() { return type; }
	public HttpServletRequest getRequest() { return request; }
	
	/** 
	 * 	Adds warning message to request <br \>
	 * 
	 * 	Messages are sometimes sent along response, so do not include critical information.
	 * 	This is a useful place to log non-critical issue found while parsing request. 
	 */
	public void addWarnings(String warning) { warnings.add(warning); }
	public String[] getWarnings() { return warnings.toArray(new String[0]); }

	/**	Get all mapped values from the request-header */
	public Map<String, String> getHeadersMap() {
		HashMap<String, String> headers = new HashMap<>();
		Enumeration<String> headerNames = request.getHeaderNames();
		
		while(headerNames.hasMoreElements()) {
			String header = headerNames.nextElement();
			headers.put(header, request.getHeader(header));
		}
		
		return headers;
	}
	
	/**
	 * 	Enum used to represent the different types of accepted HTTP Request-Methods <br />
	 * 
	 * 	See <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods">
	 * 		https://developer.mozilla.org/en-US/docs/Web/HTTP/Methods
	 *  </a> for more info
	 */
	public enum MethodType {
		GET, POST, PUT, DELETE;
	}
}
