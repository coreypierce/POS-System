package edu.wit.se16.system.logging.filters;

import java.io.OutputStream;
import java.util.function.Predicate;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import edu.wit.se16.system.logging.LogFilter;

public abstract class Logger extends OutputStreamAppender<ILoggingEvent> {
	private static final String DEFAULT_PATTERN = "%11date{h:mm:ss a} %-5level: %message%n";//  [ %thread ]

	private static PatternLayout patternLayout(String pattern) {
		PatternLayout layout = new PatternLayout();
		layout.setPattern(pattern);
		return layout;
	}
	
	private OutputStream output;
	private Layout<ILoggingEvent> layout;
	
	Logger(OutputStream output) { this(DEFAULT_PATTERN, output); }
	Logger(String pattern, OutputStream output) { this(patternLayout(pattern), output); }
	
	Logger(Layout<ILoggingEvent> layout, OutputStream output) { 
		super(); 
		
		this.output = output;
		this.layout = layout;
	}
	
	public Logger start(String name, LoggerContext context) {
		super.setName(name);
		super.setContext(context);

		layout.setContext(context);
		layout.start();
		
		super.setLayout(layout);
		super.setOutputStream(output);

		init();
		
		super.start();
		return this;
	}

	public Logger filter(LogFilter filter) { super.addFilter(new LogFilterWrapper(filter)); return this; }
	public Logger predicate(Predicate<ILoggingEvent> filter) { super.addFilter(new LogFilterWrapper(filter)); return this; }
	
	private static class LogFilterWrapper extends Filter<ILoggingEvent> {
		private LogFilter filter;
		private Predicate<ILoggingEvent> predicate;
		
		public LogFilterWrapper(LogFilter filter) { this.filter = filter; }
		public LogFilterWrapper(Predicate<ILoggingEvent> predicate) { this.predicate = predicate; }
		
		public FilterReply decide(ILoggingEvent event) { 
			return filter != null ? 
					filter.decide(event) :
					predicate.test(event) ? FilterReply.NEUTRAL : FilterReply.DENY; 
		}
	}

	protected abstract void init();
}
