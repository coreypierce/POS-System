package edu.wit.se16.system.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUtil {
	public static Logger getLogger() {
		// stack[0] = Thread.getStackTrace
		// stack[1] = LoggingUtil.getLogger (this functions)
		// stack[2] = <invoking function>
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		
		// Get the name of the class that invoked this method
		String className = stack[2].getClassName();
		
		return LoggerFactory.getLogger(className);
	} 
}
