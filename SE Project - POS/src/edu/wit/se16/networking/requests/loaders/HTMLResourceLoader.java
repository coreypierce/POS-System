package edu.wit.se16.networking.requests.loaders;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import edu.wit.se16.networking.SessionManager;
import edu.wit.se16.system.logging.LoggingUtil;

public class HTMLResourceLoader {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	// matches custom import syntax of: <import src="some_file"/>
	private static final Pattern IMPORT_PATTERN = Pattern.compile("<import\\s+(.*?)(?:\\/>|>(.*)<\\/import>)", 
																			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	// matches custom value syntax of: <value name="some_name" default="value" override="false"/>
	private static final Pattern VALUE_DEFAULT_PATTERN = Pattern.compile("<value\\s+(.*?)(?:\\/>|>(.*)<\\/value>)", 
																			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	// matches custom res syntax of: <res name="some_name" default="value" override="false"/>
	private static final Pattern RES_DEFAULT_PATTERN = Pattern.compile("<res\\s+(.*?)(?:\\/>|>(.*)<\\/res>)", 
																			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	// matches standard HTML-tag attributes: name="value"
	private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\s*([a-z]\\w*?)\\s*?=\\s*?\"(.*?)\"", 
																			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	// matches custom value pattern: {!value_name}
	private static final Pattern VALUE_PATTERN = Pattern.compile("\\{!\\s*([a-z]\\w*)\\s*\\}", 
																			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	public static InputStream loadHTMLStream(String resourcePath, CaseInsensitiveMap values) throws IOException {
		StringBuilder html = loadHTML(resourcePath, values);
		byte[] data = html.toString().getBytes();
		return new ByteArrayInputStream(data);
	}
	
	public static StringBuilder loadHTML(String resourcePath, CaseInsensitiveMap values) throws IOException {
		return loadHTML(resourcePath, values, null);
	}
	
	@SuppressWarnings("unchecked")
	private static StringBuilder loadHTML(String resourcePath, CaseInsensitiveMap values, HashMap<String, String> res) throws IOException {
		LOG.trace("Attempting to load HTML from \"{}\"...", resourcePath);
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		URL resource = loader.getResource(resourcePath);
		
		if(resource == null) {
			LOG.error("cannot find HTML file \"{}\"", resourcePath);
			throw new FileNotFoundException(resourcePath);
		}
		
		// reads the resource into a Buffer
		ByteBuffer rawData = ByteBuffer.wrap(IOUtils.toByteArray(resource));
		CharBuffer fileData = StandardCharsets.UTF_8.decode(rawData);
		StringBuilder htmlSource = new StringBuilder(fileData);
		
		// find the root-path if more resources need to be loaded
//		int pathSegment = resourcePath.lastIndexOf("/");
//		if(pathSegment < 0) pathSegment = resourcePath.length();
//		
//		String rootPath = resourcePath.substring(0, pathSegment);
//		rootPath = StringUtils.appendIfMissing(rootPath, "/");

		if(values == null) {
			values = new CaseInsensitiveMap();
		}
		
		// add session values to map
		CaseInsensitiveMap session_values = SessionManager.getSessionHTMLValues();
		if(session_values != null) session_values.forEach(values::putIfAbsent);
		
		HashMap<String, String> resources = res;
		if(resources == null) {
			resources = new HashMap<>();
		}
		
		boolean changed = true;
		
		// replace custom HTML syntax
		while(changed) {
			changed = matchDefaults(htmlSource, values);
			changed = matchValue(htmlSource, values) || changed;
			changed = matchResource(htmlSource, resourcePath, resources) || changed;
			changed = matchImports(htmlSource, resourcePath, values, resources) || changed;
		}
		
		// only if this was the first/top page
		if(res == null) {
			// add resources into page-head
			insertResources(htmlSource, resources);
		}
		
		return htmlSource;
	}
	
	/**
	 * 	Searches for and replaces custom HTML &lt;value ...&gt; tag in provided {@code StringBuilder} 
	 * 
	 * 	@return
	 * 		returns if changes were made to the provided source 
	 */
	private static boolean matchDefaults(StringBuilder htmlSource, CaseInsensitiveMap values) throws IOException {
		return matchCustom(htmlSource, "value", VALUE_DEFAULT_PATTERN, (builder, attributes) -> {
			String name = (String) attributes.get("name");
			
			String body = (String) attributes.get("body");
			String def = (String) attributes.get("default");
			
			@SuppressWarnings("unchecked")
			String override = String.valueOf(attributes.getOrDefault("override", "false"));
			
			// check name attribute
			if(name == null) {
				LOG.warn("<value ...> is missing a \"name\" attribute; tag will be ignored"); 
				return false;
			}
			
			// check default/body attribute
			if(body == null && def == null) {
				LOG.debug("No default-value provided to <value name=\"{}\"...>; tag will be ignored", name);
				return false;
				
			} else if(body != null && def != null) {
				LOG.debug("Multiple default-value provided to <value name=\"{}\"...>; attribute-value will be used", name);
				body = null;
			}
			
			// copy body to default, if value is missing
			if(def == null) def = body;
			
			// check if override value was provided
			if(values.containsKey(name)) {
				// if this tag overrides the provided value
				if(Boolean.parseBoolean(override)) {
					LOG.trace("Overriding [{}]=\"{}\" with \"{}\"", name, values.get(name), def);
					
				} else {
					LOG.trace("Value for [{}] already exists", name);
					return false;
				}
			}
			
			values.put(name, def);
			return false;
		});
	}
	
	/**
	 * 	Searches for and replaces values-markers in provided {@code StringBuilder} 
	 * 
	 * 	@return
	 * 		returns if changes were made to the provided source
	 */
	private static boolean matchValue(StringBuilder htmlSource, CaseInsensitiveMap values) {
		StringBuilder builder = new StringBuilder(htmlSource.length());
		int index = 0;

		boolean changed = false;
		
		// find and replace all value
		Matcher matcher = VALUE_PATTERN.matcher(htmlSource);
		while(matcher.find()) {
			// copy all text before the match
			builder.append(htmlSource.substring(index, matcher.start()));
			
			// extract key, and lookup value
			String key = matcher.group(1);
			String replaceValue = String.valueOf(values.get(key));
			
			LOG.trace("Replacing key [{}] with value \"{}\"", key, replaceValue == null ? "" : replaceValue);
			
			if(replaceValue != null) {
				builder.append(replaceValue);
			}
			
			changed = true;
			
			// update index to end of match
			index = matcher.end();
		}
		
		// append remaining file
		builder.append(htmlSource.substring(index));
		
		// copy back to original builder
		htmlSource.setLength(0);
		htmlSource.append(builder);
		
		return changed;
	}
	
	/**
	 * 	Searches for and replaces custom HTML &lt;res ...&gt; tag in provided {@code StringBuilder} 
	 * 
	 * 	@return
	 * 		returns if changes were made to the provided source
	 */
	private static boolean matchResource(StringBuilder htmlSource, String resource_path, HashMap<String, String> res) throws IOException {
		return matchCustom(htmlSource, "res", RES_DEFAULT_PATTERN, (builder, attributes) -> {
			String src = (String) attributes.remove("src");
			String type = (String) attributes.remove("type");
			
			// attempt to load specified source
			if(src == null) {
				LOG.warn("<res ...> is missing a \"src\" attribute; resource will be skipped"); 
				return false;
			}
			
			// convert source into absolute-path
			src = RelativePathUtil.calculatePath(resource_path, src);
			// remove "root/" from full-path to get absolute-resource path
			src = src.substring(src.indexOf('/'));

			// check to see if we already have the resource added
			if(res.containsKey(src)) {
				LOG.trace("Resource \"{}\" has already been added", src); 
				return false;
			}
			
			// validate type information 
			if(type == null) {
				LOG.warn("<res ...> is missing a \"type\" attribute; resource will be skipped"); 
				return false;
			
			} else {
				type = type.toLowerCase();
				
				switch(type) {
					case "js": case "javascript":
						LOG.trace("Attaching js-resource \"{}\"...", src);
						res.put(src, String.format("<script src=\"%s\" type=\"text/javascript\"> </script>", src));
						return true;
						
					case "css": case "less": 
					case "style": case "stylesheet":
						LOG.trace("Attaching css-resource \"{}\"...", src);
						res.put(src, String.format("<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\" />", src));
						return true;
				}
				
				LOG.warn("<res ...> has an unknown \"type\" attribute, value [{}]; resource will be skipped", type); 
				return false;
			}
		});
	}
	
	/**
	 * 	Searches for and replaces custom HTML &lt;import ...&gt; tag in provided {@code StringBuilder} 
	 * 
	 * 	@return
	 * 		returns if changes were made to the provided source
	 */
	@SuppressWarnings("unchecked")
	private static boolean matchImports(StringBuilder htmlSource, String resource_path, CaseInsensitiveMap values, 
			HashMap<String, String> res) throws IOException {
		return matchCustom(htmlSource, "import", IMPORT_PATTERN, (builder, attributes) -> {
			// clone the map, so we can add our own "local" values
			CaseInsensitiveMap values_local = new CaseInsensitiveMap(values);
			
			String src = (String) attributes.remove("src");
			String body = (String) attributes.remove("body");
			
			// if a body was provided, add it to local-values
			if(body != null) {
				// log value overwrites
				if(values_local.containsKey("body"))
					LOG.debug("Overwriting value of body");
				values_local.put("body", body);
			}
			
			for(String arg_name : (Set<String>) attributes.keySet()) {
				String arg_value = String.valueOf(attributes.get(arg_name));
				
				// log value overwrites
				if(values_local.containsKey(arg_name))
					LOG.debug("Overwriting [{}]=\"{}\" with \"{}\"", arg_name, values_local.get(arg_name), arg_value);
				// record value specified by attribute
				values_local.put(arg_name, arg_value);
			}
			
			// attempt to load specified source
			if(src == null) {
				LOG.warn("<import ...> is missing a \"src\" attribute; tag will be ignored"); 
				return false;
				
			} else {
				// convert source into full-path
				src = RelativePathUtil.calculatePath(resource_path, src);
				LOG.trace("Importing \"{}\"...", src);
				
				try {
					// load specified file, assume to be HTML, but should be fine loading .txt or similar plain-text
					builder.append(loadHTML(src, values_local, res));
				} catch (Exception e) {
					LOG.error("Failed to import HTML document!", e);
					return false;
				}
				
				return true;
			}
		});
	}
	
	private static boolean matchCustom(StringBuilder htmlSource, String tagName, Pattern pattern, 
			BiFunction<StringBuilder, CaseInsensitiveMap, Boolean> handler) throws IOException {
		
		StringBuilder builder = new StringBuilder(htmlSource.length());
		int index = 0;
		
		boolean changed = false;
		
		// find and replace all import tags
		Matcher matcher = pattern.matcher(htmlSource);
		while(matcher.find()) {
			// copy all text before the match
			builder.append(htmlSource.substring(index, matcher.start()));
			
			String args = matcher.group(1);
			String body = matcher.group(2);

			CaseInsensitiveMap attributes = new CaseInsensitiveMap();
			
			// if a body was provided, add it to local-values
			if(body != null) {
				attributes.put("body", body);
			}
			
			// attempt to process attributes
			if(args != null) {
				// Match attributes from import-tag
				Matcher args_matcher = ATTRIBUTE_PATTERN.matcher(args);
				while(args_matcher.find()) {
					String arg_name = args_matcher.group(1);
					String arg_value = args_matcher.group(2);
					
					if(arg_name == null) {
						LOG.warn("Invalid attribute on <{} ...>; attribute will be ignored", tagName); 
					
					} else {
						// log value overwrites
						if(attributes.containsKey(arg_name))
							LOG.debug("Duplicate attribute \"{}\" on <{} ...>", arg_name, tagName);
						// record value specified by attribute
						attributes.put(arg_name, arg_value);
					}
				}
			} 
			
			changed = handler.apply(builder, attributes) || changed;
			
			// update index to end of match
			index = matcher.end();
		}
		
		// append remaining file
		builder.append(htmlSource.substring(index));
		
		// copy back to original builder
		htmlSource.setLength(0);
		htmlSource.append(builder);
		
		return changed;
	}
	
	/**
	 * 	Looks for, or adds, page &lt;head&gt; tag and inserts resource tags
	 */
	private static void insertResources(StringBuilder htmlSource, HashMap<String, String> res) {
		// if there are resources
		if(res.size() <= 0) return;
		
		// collect and sort resource lines
		ArrayList<String> resources = new ArrayList<>(res.values());
		resources.sort(null);
		
		StringBuilder head_builder = new StringBuilder();
		String tag_start = resources.get(0).substring(1, 5);
		
		for(String res_line : resources) {
			// check for change in tag type
			String str = res_line.substring(1, 5);
			if(!tag_start.equals(str)) {
				tag_start = str;
				// add blank line if on new resource-type
				head_builder.append('\n');
			}

			head_builder.append('\t');
			head_builder.append(res_line);
			head_builder.append('\n');
		}
		
		String html_str = htmlSource.toString().toLowerCase();
		int insertIndex = html_str.indexOf("<head>");
		
		// if there is no head tag in the source
		if(insertIndex < 0) {
			head_builder.insert(0, "<head>");
			head_builder.append("</head>");
			
			// look for the opening HTML-tag
			insertIndex = html_str.indexOf("<html>");
			if(insertIndex >= 0) insertIndex += 6;
			else insertIndex = 0;
			
		} else {
			// find closing head-tag
			insertIndex = html_str.indexOf("</head>");
			
			// if closing-head tag is missing
			if(insertIndex < 0) {
				LOG.error("Opening <head> found, but closing tag is missing! Failed to add resources");
				return;
			}
		}
		
		// insert resources into document-head
		htmlSource.insert(insertIndex, head_builder);
	}

//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private HTMLResourceLoader() { }
}
