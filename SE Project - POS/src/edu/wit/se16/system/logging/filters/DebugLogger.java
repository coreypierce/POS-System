package edu.wit.se16.system.logging.filters;

import java.io.OutputStream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.FilterReply;

public class DebugLogger extends Logger {
	
	public DebugLogger(OutputStream output) { super(output); }
	public DebugLogger(String pattern, OutputStream output) { super(pattern, output); }
	public DebugLogger(Layout<ILoggingEvent> layout, OutputStream output) { super(layout, output); }

	protected void init() {
		super.filter(event -> 
			event.getLoggerName().startsWith("org.eclipse.jetty") ? FilterReply.DENY : 
			event.getLevel() == Level.DEBUG ? FilterReply.NEUTRAL : FilterReply.DENY
		);
	}
}