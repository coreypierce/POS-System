package edu.wit.se16.system;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import edu.wit.se16.database.Database;
import edu.wit.se16.system.logging.LoggingUtil;

public class CommandHandler {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final Pattern SQL_CMD = Pattern.compile("sql\\s\\[(.*)\\]", Pattern.CASE_INSENSITIVE);
	
	public CommandHandler() {
		Thread thread = new Thread(this::run, "Server Command - Thread");
		thread.start();
	}
	
	private void handleCommand(String cmd) {
		cmd = cmd.toLowerCase();
		
		switch(cmd) {
			case "exit":
			case "stop":
			case "shutdown":
				LOG.info("Shutting Down System...");
				RunCycle.startShutdown();
				return;
		}
		
		Matcher matcher;
		
		matcher = SQL_CMD.matcher(cmd);
		if(matcher.find()) {
			executeSQL(matcher.group(1));
			return;
		}

		LOG.warn("unknown command: {}", cmd);
	}
	
	private static void executeSQL(String sql) {
		try(Statement statement = Database.getConnection().createStatement()) {
			// execute the query
			boolean hasResults = statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
			ResultSet results = statement.getResultSet();
			
			if(hasResults) {
				if(results != null) {
					ResultSetMetaData metadata = results.getMetaData();

					StringBuilder disp = new StringBuilder();
					for(int i = 1; i <= metadata.getColumnCount(); i ++) {
						String columnName = metadata.getColumnName(i);
						int colWidth = metadata.getColumnDisplaySize(i);
						
						disp.append(StringUtils.center(columnName, colWidth));
						disp.append("|");
					}
					
					LOG.info(disp.toString());
					LOG.info(StringUtils.repeat('-', disp.length()));
					
					while(results.next()) {
						// clear buffer
						disp.setLength(0);
						
						for(int i = 1; i <= metadata.getColumnCount(); i ++) {
							int colWidth = metadata.getColumnDisplaySize(i);
							disp.append(StringUtils.leftPad(results.getString(i), colWidth));
							disp.append("|");
						}

						LOG.info(disp.toString());
					}
				} else {
					LOG.info("SQL successful [No rows returned]");
				}
			} else {
				LOG.info("SQL successful [{} row(s) updated]", statement.getUpdateCount());
			}
		} catch(SQLException e) {
			LOG.error("SQL Failed!\n {}", e.getMessage());
		}
	} 
	
	private void run() {
		try {
			byte[] buffer = new byte[4096];
			int read;
			
			while((read = System.in.read(buffer)) > 0) {
				handleCommand(new String(buffer, 0, read));
			}
		} catch(IOException e) {
			// most likely stream-closed
			LOG.info("input-stream has closed");
		}
	}
}
