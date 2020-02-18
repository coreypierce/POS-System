package edu.wit.se16.networking;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import edu.wit.se16.networking.requests.RequestInfo;
import edu.wit.se16.networking.requests.loaders.HTMLResourceLoader;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.JsonBuilder;

public class StandardResponses {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	/**
	 * 	Response to provided request with a JSON-Object containing:
	 * 		<ul>
	 * 			<li> error: &lt;error-message&gt; </li>
	 * 			<li> status: "error" </li>
	 * 			<li> warnings: &lt;see request&gt; </li>
	 * 		</ul>
	 * 	Method also sets http-response code to provided {@code errorCode}, along with content-headers <br />
	 */
	public static HttpServletResponse error(RequestInfo request, HttpServletResponse response, int errorCode, String errorMessage) throws IOException {
		JsonBuilder.create()
			.append("error", errorMessage)
			.append("status", "error")
			.append("warnings", request != null ? request.getWarnings() : new String[0])
		.build(response);

		response.setStatus(errorCode);
		return response;
	}
	
	/**
	 * 	Response to provided request with default "404-Missing" HTML page 
	 */
	public static HttpServletResponse missing(RequestInfo request, HttpServletResponse response, String filename) throws IOException, ServletException {
		CaseInsensitiveMap values = new CaseInsensitiveMap();
		values.put("missing_name", filename);
		
		try {
			InputStream in = HTMLResourceLoader.loadHTMLStream("edu/wit/se16/networking/Missing404.html", values);
			OutputStream out = response.getOutputStream();
			
			// set content headers
			response.setContentType("text/html");
			response.setContentLength(in.available());
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			
			// copy all bytes from file-in to http-out
			IOUtils.copy(in, out);
			
			// clean up resources
			out.close();
			in.close();
		} catch(FileNotFoundException e) {
			LOG.error("Failed to load 404 HTML file! Check resource path");
		}
		
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		return response;
	}

//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private StandardResponses() { }
}
