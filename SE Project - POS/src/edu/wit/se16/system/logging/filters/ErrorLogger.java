package edu.wit.se16.system.logging.filters;

import java.io.OutputStream;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.spi.FilterReply;

public class ErrorLogger extends Logger {
	
	public ErrorLogger(OutputStream output) { super(output); }
	public ErrorLogger(String pattern, OutputStream output) { super(pattern, output); }
	public ErrorLogger(Layout<ILoggingEvent> layout, OutputStream output) { super(layout, output); }

	protected void init() {

		super.filter(event -> event.getLevel() == Level.ERROR ? FilterReply.NEUTRAL : FilterReply.DENY);
	}
}