package edu.wit.se16.system.logging.console;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import edu.wit.se16.system.LocalVars;
import edu.wit.se16.system.RunCycle;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.system.logging.console.StreamTypes.MessageFormatedOutputStream;
import edu.wit.se16.system.logging.console.StreamTypes.PipeStreamFactory;
import edu.wit.se16.system.logging.console.StreamTypes.SplitOutputStream;
import edu.wit.se16.system.logging.filters.DebugLogger;
import edu.wit.se16.system.logging.filters.ErrorLogger;
import edu.wit.se16.system.logging.filters.InfoLogger;
import edu.wit.se16.system.logging.filters.TraceLogger;
import edu.wit.se16.system.logging.filters.WarningLogger;
import edu.wit.se16.util.ErrorUtil;

public class LogConsoleStreamProcessor {
	private static final org.slf4j.Logger LOG = LoggingUtil.getLogger();
	
	// ===================================== Stream Constants ===================================== \\
	
	private static final int STREAM_BUFFER_SIZE = 4096;
	private static final int READ_BUFFER_SIZE = 512;
	
	private static final String MESSAGE_SUFFIX = "`=`";
	private static final String STACK_MARKER = "~~`";

	// ===================================== Style Constants ===================================== \\
	
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm:ss a");
	
	private static final int DATE_TIME_WIDTH = 11;
	private static final int LEVEL_WIDTH = 5;
	
	private static final String DEFAULT_FONT_FAMILY = "Consolas";
	private static final int DEFAULT_FONT_SIZE = 12;
	
	// ===================================== INFO Text-Style ===================================== \\
	
	private static final SimpleAttributeSet INFO_STYLE = new SimpleAttributeSet(); static {
		StyleConstants.setFontFamily(INFO_STYLE, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(INFO_STYLE, DEFAULT_FONT_SIZE);
		
		StyleConstants.setForeground(INFO_STYLE, Color.BLACK);
		StyleConstants.setBackground(INFO_STYLE, Color.WHITE);
	}

	// ===================================== WARNING Text-Style ===================================== \\
	
	private static final SimpleAttributeSet WARN_STYLE = new SimpleAttributeSet(); static {
		StyleConstants.setFontFamily(WARN_STYLE, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(WARN_STYLE, DEFAULT_FONT_SIZE);
		
		StyleConstants.setForeground(WARN_STYLE, new Color(228, 182, 30));
		StyleConstants.setBackground(WARN_STYLE, Color.WHITE);
	}
	
	// ===================================== ERROR Text-Style ===================================== \\
	
	private static final SimpleAttributeSet ERROR_STYLE = new SimpleAttributeSet(); static {
		StyleConstants.setFontFamily(ERROR_STYLE, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(ERROR_STYLE, DEFAULT_FONT_SIZE);
		
		StyleConstants.setForeground(ERROR_STYLE, Color.RED);
		StyleConstants.setBackground(ERROR_STYLE, new Color(228, 228, 228));
	}
	
	// ===================================== DEBUG Text-Style ===================================== \\
	
	private static final SimpleAttributeSet DEBUG_STYLE = new SimpleAttributeSet(); static {
		StyleConstants.setFontFamily(DEBUG_STYLE, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(DEBUG_STYLE, DEFAULT_FONT_SIZE);
		
		StyleConstants.setForeground(DEBUG_STYLE, new Color(77, 123, 151));
		StyleConstants.setBackground(DEBUG_STYLE, new Color(228, 228, 228));
	}
	
	// ===================================== TRACE Text-Style ===================================== \\
	
	private static final SimpleAttributeSet TRACE_STYLE = new SimpleAttributeSet(); static {
		StyleConstants.setFontFamily(TRACE_STYLE, DEFAULT_FONT_FAMILY);
		StyleConstants.setFontSize(TRACE_STYLE, DEFAULT_FONT_SIZE);
		
		StyleConstants.setForeground(TRACE_STYLE, new Color(150, 150, 150));
		StyleConstants.setBackground(TRACE_STYLE, Color.WHITE);
	}
	
	// ===================================== INPUT Text-Style ===================================== \\
	
		private static final SimpleAttributeSet INPUT_STYLE = new SimpleAttributeSet(); static {
			StyleConstants.setFontFamily(INPUT_STYLE, DEFAULT_FONT_FAMILY);
			StyleConstants.setFontSize(INPUT_STYLE, DEFAULT_FONT_SIZE);
			
			StyleConstants.setForeground(INPUT_STYLE, new Color(108, 166, 15)); //new Color(123, 189, 17)
			StyleConstants.setBackground(INPUT_STYLE, Color.WHITE);
		}
	
	// ===================================== Streams ===================================== \\
	
	private OutputStream info_write;
	private OutputStream warn_write;
	private OutputStream error_write;
	private OutputStream debug_write;
	private OutputStream trace_write;

	private InputStream info_read;
	private InputStream warn_read;
	private InputStream error_read;
	private InputStream debug_read;
	private InputStream trace_read;
	
	private OutputStream input_write;
	private InputStream input_read;
	
	private OutputStream log_file_write;
	
	private static LogConsoleStreamProcessor input_master;
	
	// ===================================== Filter ===================================== \\
	
	private boolean show_info, show_warn, show_error, show_debug, show_trace;
	private Pattern filter;
	
	// ===================================== Document ===================================== \\

	private int longestLine;
	private boolean lineWrap = ConsoleVars.CONSOLE_LINE_WRAP;
	
	public void setLineWrap(boolean wrap) { 
		this.lineWrap = wrap; 
		rebuildDocument(); 
	}
	
	private boolean showStack = ConsoleVars.CONSOLE_SHOW_STACKTRACE;
	
	public void setShowStack(boolean show) { 
		this.showStack = show; 
		rebuildDocument(); 
	}
	
	private int width;
	private List<LogMessage> lines;
	private DefaultStyledDocument document;
	
	public StyledDocument getDocumnet() { return document; }
	
	// ===================================== Setup ===================================== \\
	
	public LogConsoleStreamProcessor() {
		// init document that logs will be written to
		document = new DefaultStyledDocument();
		lines = new ArrayList<>();
		
		initLogLevels();
		initThreads();
	}
	
	private void initLogLevels() {
		try {
			// setup connected java.io streams for each log-level 
			PipeStreamFactory info_streams = new PipeStreamFactory(STREAM_BUFFER_SIZE);
			PipeStreamFactory warn_streams = new PipeStreamFactory(STREAM_BUFFER_SIZE);
			PipeStreamFactory error_streams = new PipeStreamFactory(STREAM_BUFFER_SIZE);
			PipeStreamFactory debug_streams = new PipeStreamFactory(STREAM_BUFFER_SIZE);
			PipeStreamFactory trace_streams = new PipeStreamFactory(STREAM_BUFFER_SIZE);
			
			 info_read =  info_streams.getInputStream();  info_write =  info_streams.getOutputStream(); 
			 warn_read =  warn_streams.getInputStream();  warn_write =  warn_streams.getOutputStream(); 
			error_read = error_streams.getInputStream(); error_write = error_streams.getOutputStream(); 
			debug_read = debug_streams.getInputStream(); debug_write = debug_streams.getOutputStream(); 
			trace_read = trace_streams.getInputStream(); trace_write = trace_streams.getOutputStream(); 

			PipeStreamFactory input_streams = new PipeStreamFactory(STREAM_BUFFER_SIZE / 4);
			input_read = input_streams.getInputStream(); input_write = input_streams.getOutputStream();
			
		} catch(IOException ignore) {}
		
		// get root logger
		Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		// remove default console output
		root.detachAppender("console");
		// force all levels on
		root.setLevel(Level.ALL);
		root.setAdditive(true);
		
		// Create loggers
		root.addAppender(new InfoLogger(new MessageLayout(), info_write).start("Console - INFO", context));
		root.addAppender(new WarningLogger(new MessageLayout(), warn_write).start("Console - WARN", context));
		root.addAppender(new ErrorLogger(new MessageLayout(), error_write).start("Console - ERROR", context));
		root.addAppender(new DebugLogger(new MessageLayout(), debug_write).start("Console - DEBUG", context));
		root.addAppender(new TraceLogger(new MessageLayout(), trace_write).start("Console - TRACE", context));
		
		setupLogFile(root, context);
		
		// redirect System streams to log-level streams
		OutputStream info_message = new MessageFormatedOutputStream(info_write, LogConsoleStreamProcessor::formateMessage, MESSAGE_SUFFIX);
		OutputStream err_message = new MessageFormatedOutputStream(error_write, LogConsoleStreamProcessor::formateMessage, MESSAGE_SUFFIX);
		
		System.setOut(new PrintStream(new SplitOutputStream(System.out, info_message), true));
		System.setErr(new PrintStream(new SplitOutputStream(System.err, err_message), true));
		
		System.setIn(input_read);
		RunCycle.addPreShutdownAction(() -> ErrorUtil.ignore(input_write::close));
		
		// record which LogConsole has the other end of the input-stream
		input_master = this;
	}

	private void setupLogFile(Logger root, LoggerContext context) {
		long fileID = Instant.now().getEpochSecond();
		String filePath = LocalVars.ROOT_FOLDER + "/logs/log_" + fileID + ".txt";
		
		File _file = new File(filePath);
		_file.getParentFile().mkdirs();
		
		try {
			log_file_write = new FileOutputStream(_file);
			
		} catch(IOException e) { 
			LOG.error("Couldn't create log file!", e);
		}
	}
	
	private void initThreads() {
		// setup threads to process streams
		Thread info_thread = new Thread(new StreamHandler(info_read, this::handleInfo), "info_log - thread");
		Thread warn_thread = new Thread(new StreamHandler(warn_read, this::handleWarn), "warn_log - thread");
		Thread error_thread = new Thread(new StreamHandler(error_read, this::handleError), "error_log - thread");
		Thread debug_thread = new Thread(new StreamHandler(debug_read, this::handleDebug), "debug_log - thread");
		Thread trace_thread = new Thread(new StreamHandler(trace_read, this::handleTrace), "trace_log - thread");
		
		// mark threads as daemon-threads
		info_thread.setDaemon(true);
		warn_thread.setDaemon(true);
		error_thread.setDaemon(true);
		debug_thread.setDaemon(true);
		trace_thread.setDaemon(true);
		
		// start logging threads
		info_thread.start();
		warn_thread.start();
		error_thread.start();
		debug_thread.start();
		trace_thread.start();
	}
	
	// ===================================== Input Methods ===================================== \\

	public void writeInputMessage(String message) {
		try {
			appendMessage(new LogMessage(Level.ALL, formateMessage(message)));
		} catch (BadLocationException ignore) { }
	}
	
	public static void sendInput(String message) {
		try {
			input_master.input_write.write(message.getBytes(StandardCharsets.UTF_8));
		} catch (IOException ignore) { }
	}
	
	// ===================================== Document Updates ===================================== \\
	
	public void documnetContainerResized(JTextPane textPane, int pane_width) {
		Graphics g = textPane.getGraphics();
		FontMetrics metrics = g.getFontMetrics(new Font(DEFAULT_FONT_FAMILY, Font.PLAIN, DEFAULT_FONT_SIZE));
		int char_width = metrics.charWidth('_');
		
		this.width = pane_width / char_width - 1;
		rebuildDocument();
	}
	
	public synchronized void rebuildDocument() {
		try {
			// start by clearing the document
			document.remove(0, document.getLength());
			
			lines.stream()
				.filter(this::filter)	// filter log messages by log-level
				.sorted()
				.forEach(e -> ErrorUtil.ignore(
					() -> e.insert(document, lineWrap ? width : longestLine, showStack, document.getLength())
				));
		} catch (BadLocationException e) { }
	}
	
	private synchronized void appendMessage(LogMessage message) throws BadLocationException {
		lines.add(message);
		
		try {
			// attempt to write log-message to file
			if(log_file_write != null) {
				log_file_write.write(message.rawMessage.getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			ErrorUtil.ignore(log_file_write::close);
			LOG.warn("Log-File ends here!");
			LOG.error("Log-File has experianced and error", e);
		}
		
		// update line-length statistics
		if(message.lineLength > longestLine) {
			longestLine = message.lineLength + 3;
			if(!lineWrap) rebuildDocument();
		}
		
		// check if line should be displayed, given current filters
		if(filter(message)) {
			message.insert(document, lineWrap ? width : longestLine, showStack, document.getLength());
		}
	}
	
	public void clear() {
		lines.clear();
		rebuildDocument();
	}
	
	public void updateFilters(boolean info, boolean warn, boolean error, boolean debug, boolean trace, String filter) {
		this.show_info = info;
		this.show_warn = warn;
		this.show_error = error;
		this.show_debug = debug;
		this.show_trace = trace;
		
		// filter always comes in with an extra "/." on the end
		
		int flags = 0;
		if(filter.endsWith("/i")) {
			flags = Pattern.CASE_INSENSITIVE;
		}

		filter = filter.substring(0, filter.length() - 2);
		if(filter.isEmpty()) filter = ".*";
		this.filter = Pattern.compile(filter, flags);
		
		rebuildDocument();
	}
	
	private boolean filter(LogMessage msg) {
		switch(msg.level.levelInt) {
			case Level.ALL_INT: return true;
			
			case Level.INFO_INT: if(!show_info) return false; break;
			case Level.WARN_INT: if(!show_warn) return false; break;
			case Level.ERROR_INT: if(!show_error) return false; break;
			case Level.DEBUG_INT: if(!show_debug) return false; break;
			case Level.TRACE_INT: if(!show_trace) return false; break;
		}
		
		return filter.matcher(msg.message).find();
	}
	
	// ===================================== Message Handlers ===================================== \\
	
	private synchronized void handleInfo(String message) {
		try {
			appendMessage(new LogMessage(Level.INFO, message));
//			new LogMessage(Level.INFO, message).insert(document, width, document.getLength());
//			document.insertString(document.getLength(), message, INFO_STYLE);
		} catch (BadLocationException ignore) { }
	}
	
	private synchronized void handleWarn(String message) {
		try {
			appendMessage(new LogMessage(Level.WARN, message));
//			new LogMessage(Level.WARN, message).insert(document, width, document.getLength());
//			document.insertString(document.getLength(), message, WARN_STYLE);
		} catch (BadLocationException ignore) { }
	}

	private synchronized void handleError(String message) {
		try {
			appendMessage(new LogMessage(Level.ERROR, message));
//			new LogMessage(Level.ERROR, message).insert(document, width, document.getLength());
//			document.insertString(document.getLength(), message, ERROR_STYLE);
		} catch (BadLocationException ignore) { }
	}
	
	private synchronized void handleDebug(String message) {
		try {
			appendMessage(new LogMessage(Level.DEBUG, message));
//			new LogMessage(Level.DEBUG, message).insert(document, width, document.getLength());
//			document.insertString(document.getLength(), message, DEBUG_STYLE);
		} catch (BadLocationException ignore) { }
	}
	
	private synchronized void handleTrace(String message) {
		try {
			appendMessage(new LogMessage(Level.TRACE, message));
//			new LogMessage(Level.TRACE, message).insert(document, width, document.getLength());
//			document.insertString(document.getLength(), message, TRACE_STYLE);
		} catch (BadLocationException ignore) { }
	}
	
	private static class LogMessage implements Comparable<LogMessage> {
		private static final Pattern LINE_PATTERN = Pattern.compile("(.*)\\s");
		private static final Pattern STACK_PATTERN = Pattern.compile(STACK_MARKER + ".*" + STACK_MARKER, Pattern.DOTALL);
		
		private Level level;
		private ZonedDateTime time;
		private String message;
		
		// Message used when writing to file
		private String rawMessage;
		private int lineLength;

		private AttributeSet style;
		
		public LogMessage(Level level, String raw_message) {
			this.level = level;
			
			switch (level.levelInt) {
				case Level.ALL_INT: style = INPUT_STYLE; break;
				case Level.INFO_INT: style = INFO_STYLE; break;
				case Level.WARN_INT: style = WARN_STYLE; break;
				case Level.ERROR_INT: style = ERROR_STYLE; break;
				case Level.DEBUG_INT: style = DEBUG_STYLE; break;
				case Level.TRACE_INT: style = TRACE_STYLE; break;
			}
			
			// set message to be used in file (remove STACK_MARKERs as they aren't need in the file)
			this.rawMessage = raw_message.replaceAll(STACK_MARKER, "");
			
			// Extract timestamp and conver to Date-Time Object
			String timeString = raw_message.substring(0, MessageLayout.DATE_TIME_WIDTH);
			time = ZonedDateTime.parse(timeString, MessageLayout.DATE_TIME_FORMATTER);
			
			// remove end whitespace or newline
			this.message = raw_message.substring(MessageLayout.MESSAGE_OFFSET).replaceAll("\\s$", "");
			this.message = message.replace('\t', ' ');

			// make sure message is 1+ characters, and starts with a space
			if(message.isEmpty()) message = " ";
			if(message.charAt(0) != ' ') message = " " + message;
			
			// calculate longest segment of message (STACK_MARKER are never shown)
			Matcher matcher = LINE_PATTERN.matcher(message.replaceAll(STACK_MARKER, ""));
			while(matcher.find()) {
				int length = matcher.group(1).length();
				if(length > lineLength) {
					lineLength = length;
				}
			}
			
			lineLength += DATE_TIME_WIDTH + LEVEL_WIDTH + 3;
		}
		
		public void insert(Document doc, int width, boolean showStack, int offset) throws BadLocationException {
			String timestamp = StringUtils.leftPad(DATE_TIME_FORMATTER.format(time), DATE_TIME_WIDTH);
			
			String msg_type = level == Level.ALL ? "INPUT" : level.levelStr;
			msg_type = " " + StringUtils.leftPad(msg_type, LEVEL_WIDTH) + ":";
			
			// calculate remaining width for message
			int message_width = width - (timestamp.length() + msg_type.length() + 1);
			if(message_width <= 0) message_width = 1;
			
			String message = this.message;
			if(!showStack) {
				message = STACK_PATTERN.matcher(message).replaceAll("<stack-trace omitted> >");
			} else {
				message = message.replaceAll(STACK_MARKER, "");
			}
			
			// message split setup
			StringBuilder multiLineString = new StringBuilder();
			Pattern widthPattern = Pattern.compile("(?:(.{1," + message_width + "})\\s|(.{" + message_width + "}))");
			Matcher matcher = widthPattern.matcher(message);
			
			// split message into segments of < [message_width] chars at nearest space
			while(matcher.find()) {
				String msg = matcher.group(1);
				if(msg == null) msg = matcher.group(2);
				msg = StringUtils.rightPad(msg, message_width);
				
				// if this the first segment
				if(matcher.start() == 0) {
					// append timestamp and log-level
					msg = timestamp + msg_type + msg + ' ';
				} else {
					// if not, left pad segment to line up
					msg = StringUtils.leftPad(msg, width);
				}
				
				multiLineString.append(msg);
				multiLineString.append('\n');
			}
			
			// insert message into document
			doc.insertString(offset, multiLineString.toString(), style);
		}

		public int compareTo(LogMessage o) {
			return time.compareTo(o.time);
		}
	}
	
	// ===================================== Stream Tread-Reader ===================================== \\
	
	private static class StreamHandler implements Runnable {
		private InputStream stream;
		private Consumer<String> processor;
		
		public StreamHandler(InputStream stream, Consumer<String> processor) {
			this.stream = stream;
			this.processor = processor;
		}
		
		public void run() {
			try {
				int read;
				byte[] buffer = new byte[READ_BUFFER_SIZE];
				StringBuffer builder = new StringBuffer();
				
				while((read = stream.read(buffer)) > 0) {
					String message = new String(buffer, 0, read);
					
					while(true) {
						// look for message suffix
						int index = message.indexOf(MESSAGE_SUFFIX);
						// if missing, break
						if(index < 0) break;
						
						// if message does contain suffix, split-off message part 
						builder.append(message.substring(0, index));
						
						// process, now completed, message
						processor.accept(builder.toString());
						builder.setLength(0); // reset buffer
						
						// trim string to non-message part
						message = message.substring(index + MESSAGE_SUFFIX.length());
					}
					
					// append remaining message to buffer
					builder.append(message);
				}
			} catch(IOException e) {
				// most likely stream-closed
				processor.accept(formateMessage("stream has closed"));
				e.printStackTrace();
			}
		}
	}
	
	//  ===================================== Message Layout ===================================== \\
	
	private static class MessageLayout extends LayoutBase<ILoggingEvent> {
		private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd;HH:mm:ss.SSS")
																		.withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault());
		private static final int DATE_TIME_WIDTH = 23;
		private static final int LOG_LEVEL_WIDTH = 5;
		private static final int MESSAGE_OFFSET = DATE_TIME_WIDTH + 1 + LOG_LEVEL_WIDTH + 3;
		
		public String doLayout(ILoggingEvent event) {
			StringBuilder builder = new StringBuilder();
			
			String level = event.getLevel().levelStr;
			Instant time = Instant.ofEpochMilli(event.getTimeStamp());
			IThrowableProxy exception = event.getThrowableProxy();
			
			// Log date
			builder.append(DATE_TIME_FORMATTER.format(time));
			builder.append(' ');
			
			// Log Level
			builder.append(StringUtils.leftPad(level, LOG_LEVEL_WIDTH));
			builder.append(" - ");
			
			// Log Message (with "printf" formatting already done)
			builder.append(event.getFormattedMessage());
			builder.append("\r\n");
			
			// Log Exception
			while(exception != null) {
				builder.append(exception.getMessage());
				builder.append("\r\n");
				
				builder.append(STACK_MARKER);
				for(StackTraceElementProxy stackElement : exception.getStackTraceElementProxyArray()) {
					// "at <stack-element>"
					builder.append("    ");
					builder.append(stackElement.getSTEAsString());
					builder.append("\r\n");
				}
				
				builder.append(STACK_MARKER);
				
				exception = exception.getCause();
				if(exception != null) {
					builder.append("Caused by: ");
				}
			}
			
			builder.append(MESSAGE_SUFFIX);
			return builder.toString();
		}
	}
	
	private static String formateMessage(CharSequence msg) {
		String timestamp = MessageLayout.DATE_TIME_FORMATTER.format(LocalDateTime.now());
		return timestamp + " LEVEL - " + msg + "\r\n";// + MESSAGE_SUFFIX;
	}
}
