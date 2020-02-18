package edu.wit.se16.networking.requests;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.loaders.HTMLResourceLoader;
import edu.wit.se16.system.logging.LoggingUtil;

public class RequestPage implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private String filename;
	private String resource;
//	private URL sourceFile;

	public RequestPage(String resource, String name) {
		this.filename = name;
		this.resource = resource;
	}

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		// open stream to HTML file, and open response stream
		InputStream in = HTMLResourceLoader.loadHTMLStream(resource, null);
		OutputStream out = response.getOutputStream();
		
		if(in != null) {
			// set content headers
			response.setContentType("text/html");
			response.setContentLength(in.available());
			
			// copy all bytes from file-in to http-out
			IOUtils.copy(in, out);
			
			// clean up resources
			out.close();
			in.close();
			
		} else { //(FileNotFoundException e) {
			LOG.warn("Unable to find HTML-file \"{}\"", resource); //sourceFile.getFile());
			return StandardResponses.missing(request, response, filename);
		}

		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}

	public String getCommand() { return filename; }
}
