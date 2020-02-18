package edu.wit.se16.networking.requests.loaders;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

import edu.wit.se16.system.logging.LoggingUtil;
import root.Automapper;

public class RelativePathUtil {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final Pattern PATH_SEPARATOR_PATTERN = Pattern.compile("[\\/]");
	private static final Pattern PARENT_DIR_PATTERN = Pattern.compile("^\\.\\.[\\/]");
	
	/**
	 * 	Used as the root-directory
	 */
	private static final String AUTOMAPPER_ROOT = Automapper.class.getPackage().getName();
	
	
	public static String calculatePath(String current_path, String reletive_path) {
		LOG.trace("Building reletive-path \"{}\" from base [{}]", reletive_path, current_path);
		
		// check for "your/parent directory"
		if(reletive_path.startsWith("./")) {
			reletive_path = reletive_path.substring(2);
		}
		
		// absolute path
		if(reletive_path.startsWith("/")) {
			return AUTOMAPPER_ROOT + reletive_path;
		}
		
		// relative path
		if(current_path.startsWith(AUTOMAPPER_ROOT)) {
			current_path = current_path.substring(AUTOMAPPER_ROOT.length());
		}
		
		if(current_path.startsWith("/")) {
			current_path = current_path.substring(1);
		}
		
		// divide current path
		String[] pathSections = PATH_SEPARATOR_PATTERN.split(current_path);
		
		// start at 1, as the "resource-name" in not needed
		int fromRootCount = 1;
		while(true) {
			// search string for: ../
			Matcher matcher = PARENT_DIR_PATTERN.matcher(reletive_path);
			if(matcher.find()) {
				fromRootCount ++;
				reletive_path = reletive_path.substring(matcher.end());
			} else {
				break;
			}
		}
		
		// rebuild path
		StringBuilder rootPath = new StringBuilder();
		rootPath.append(AUTOMAPPER_ROOT);
		
		for(int i = 0, len = pathSections.length - fromRootCount; i < len; i ++) {
			rootPath.append('/');
			rootPath.append(pathSections[i]);
		}
		
		rootPath.append('/');
		rootPath.append(reletive_path);
		return rootPath.toString();
	}

//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private RelativePathUtil() { }
}
