package edu.wit.se16.networking.requests;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import edu.wit.se16.networking.StandardResponses;
import edu.wit.se16.networking.requests.loaders.LessResourceLoader;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.ErrorUtil;

public class DefaultFileRequest implements IRequest {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private HashMap<String, SoftReference<ByteBuffer>> cache;
	private String rootFolder;
	
	public DefaultFileRequest(String rootFolder) {
		this.cache = new HashMap<>();
		this.rootFolder = rootFolder;
	}

	public HttpServletResponse process(RequestInfo request, HttpServletResponse response) throws IOException, ServletException {
		String fileRequest = request.getRequest().getPathInfo();
		String filePath = rootFolder + fileRequest;
		
		LOG.trace("Looking for resource \"{}\"", filePath);
		
		ByteBuffer data = null;
		
		// checks to see if we have a cached version of the file
		SoftReference<ByteBuffer> data_ref = cache.get(filePath);
		if(data_ref != null) {
			// if there is a cached version, make sure it hasn't been cleared
			data = data_ref.get();
			LOG.trace("Cached version found for resource \"{}\"", fileRequest);
		} 

		if(data_ref != null && data == null) {
			LOG.trace("Cached version has expired for resource \"{}\"", fileRequest);
		}
		
		// if there is no, live, version of this file
		if(data == null) {
			LOG.trace("Loading resource \"{}\"...", filePath);
			
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			URL resource = loader.getResource(filePath);
			
			if(resource != null) {
				// reads the resource into a ByteBuffer
				data = loadResource(filePath, resource);
				data.flip();
				
				// adds a reference to the buffer, into the cache
				cache.put(filePath, new SoftReference<ByteBuffer>(data));
				
				LOG.trace("Resource \"{}\" loaded, size: {} bytes", filePath, data.capacity());
						
			} else {
				LOG.warn("Unable to find resource \"{}\"", filePath);
				return StandardResponses.missing(request, response, fileRequest);
			}
		}
		
		// open response stream
		OutputStream out = response.getOutputStream();
		
		// set content headers
		response.setContentType(getContentType(fileRequest));
		response.setContentLength(data.capacity());
		
		// writes all bytes into output stream
		IOUtils.write(data.array(), out);
		out.close();

		response.setStatus(HttpServletResponse.SC_OK);
		return response;
	}
	
	/**
	 * 	Attempts to load a resource into a ByteBuffer
	 */
	private static ByteBuffer loadResource(String resourcePath, URL resource) throws IOException {
		// if the requested file is a .less file
		if(resourcePath.toLowerCase().endsWith(".less")) {
			// first compile the resource, then return it as CSS
			String compiledCSS = LessResourceLoader.loadLess(resourcePath);
			return ByteBuffer.wrap(compiledCSS.getBytes(StandardCharsets.UTF_8));
		}
		
		// if no special cases for file type
		return ByteBuffer.wrap(IOUtils.toByteArray(resource));
	}
	
	/**
	 * 	Looks up the content-type for a resource, or takes a guess
	 * 
	 * 	@see HttpServletResponse#setContentType(String)
	 * 	@see URLConnection#guessContentTypeFromName(String)
	 */
	private static String getContentType(String resourcePath) {
		// if the requested file is a .less file
		if(resourcePath.toLowerCase().endsWith(".less")) {
			return "text/css";
		}
		
		// if the requested file is a .svg file
		if(resourcePath.toLowerCase().endsWith(".svg")) {
			return "image/svg+xml";
		}

		// if no special cases for file type
		return URLConnection.guessContentTypeFromName(resourcePath);
	}

	public String getCommand() { 
		throw ErrorUtil.sneekyThrow(new IllegalAccessException("Cannot register a DefaultFileRequest")); 
	}
}
