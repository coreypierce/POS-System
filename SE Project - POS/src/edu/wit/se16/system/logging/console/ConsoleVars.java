package edu.wit.se16.system.logging.console;

class ConsoleVars {
	public static final boolean ENABLE_INFO_LOG = true;
	public static final boolean ENABLE_WARN_LOG = true;
	public static final boolean ENABLE_ERROR_LOG = true;
	public static final boolean ENABLE_DEBUG_LOG = true;
	public static final boolean ENABLE_TRACE_LOG = false;
	
	public static final boolean CONSOLE_LINE_WRAP = false;
	public static final boolean CONSOLE_SHOW_STACKTRACE = true;
	
//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private ConsoleVars() { }
}