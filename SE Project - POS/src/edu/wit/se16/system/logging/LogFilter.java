package edu.wit.se16.system.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.FilterReply;

@FunctionalInterface
public interface LogFilter { 
	public FilterReply decide(ILoggingEvent event);
}