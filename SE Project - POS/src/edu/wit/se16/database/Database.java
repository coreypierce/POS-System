package edu.wit.se16.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

import org.slf4j.Logger;

import edu.wit.se16.system.RunCycle;
import edu.wit.se16.system.SystemVars;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.ErrorUtil;

public class Database {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private static final String CONNECTOR_SUBPROTOCAL = "mysql";
	private static final String CONNECTOR_URL_PROPERTIES = "useSSL=false&";
	
	private static final String CONNECTOR_ADDRESS = 
			"jdbc:" + CONNECTOR_SUBPROTOCAL + "://" +
			SystemVars.DATABASE_HOSTNAME + ":" + SystemVars.DATABASE_PORT +
			"?" + CONNECTOR_URL_PROPERTIES;
	
// =========================================== ========== =========================================== \\
// =========================================== Connection =========================================== \\
	
	private static Connection connection;
	
	private static void connect() throws SQLException {
		connection = DriverManager.getConnection(CONNECTOR_ADDRESS, SystemVars.DATABASE_USERNAME, SystemVars.DATABASE_PASSWORD);
		RunCycle.addShutdownAction(connection::close);
		buildDatabase();
	}
	
	private static void buildDatabase() throws SQLException {
		Connection connection = getConnection();
		
		// check for "POS" database
		PreparedStatement statement = connection.prepareStatement(
				"SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'pos'");
		
		try {
			ResultSet results = statement.executeQuery();
			LOG.trace("Checking for 'POS' database...");
			
			// if the database is missing
			if(!results.next()) {
				LOG.debug("'POS' Database missing! Loading setup script...");

				ScriptRunner runner = new ScriptRunner(connection, false, false);
				InputStream script_in = Database.class.getResourceAsStream("setup.sql");
				runner.runScript(new BufferedReader(new InputStreamReader(script_in)));
				
				// TODO: Temp add test-data REMOVE BEFORE RELEASE
				LOG.warn("Temp data added; REMOVE BEFORE RELEASE");
				script_in = Database.class.getResourceAsStream("test_data.sql");
				runner.runScript(new BufferedReader(new InputStreamReader(script_in)));
			}
		} catch(IOException e) {
			LOG.error("Failed to find SQL creation script!", e);
		}

		connection.createStatement().execute("USE `pos`");
	}
	
	public static Connection getConnection() {
		try {
			if(connection == null || connection.isClosed()) {
				connect();
			}
			
			return connection;
			
		} catch (SQLException e) {
			LOG.error("Failed to open connection to Database!", e);
			throw ErrorUtil.sneekyThrow(e);
		}
	}
	

// =========================================== ========== =========================================== \\
// =========================================== Statements =========================================== \\
	
	public static PreparedStatement prep(String sql) {
		try {
			Connection connect = getConnection();
			PreparedStatement statement = connect.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			return statement;
			
		} catch (SQLException e) {
			LOG.error("Failed to create SQL-Statment!", e);
			throw ErrorUtil.sneekyThrow(e);
		}
	}
	
	@FunctionalInterface
	public static interface ResultsConsumer extends Consumer<ResultSet> {
		void process(ResultSet t) throws SQLException;
		
		default void accept(ResultSet t) {
			try {
				process(t);
			} catch (SQLException e) {
				throw ErrorUtil.sneekyThrow(e);
			}
		}
	}
	
	public static boolean query(ResultsConsumer action, PreparedStatement statement, Object... args) {
		try {
			// assign parameters as genetics (library chooses a type)
			for(int i = 0; i < args.length; i ++) {
				statement.setObject(i + 1, args[i]);
			}
			
			ResultSet results = statement.executeQuery();
			boolean hasResult = false;
			
			// if result processor was provided
			if(action != null) { 
				while(results.next()) {
					hasResult = true;
					action.accept(results);
				}
			} else {
				hasResult = results.next();
			}
			
			return hasResult;
		} catch(SQLException e) {
			// sneaky throw error, worst case: caught by HTTP thread and respond with 500
			throw ErrorUtil.sneekyThrow(e);
		}
	}
	
	public static boolean update(PreparedStatement statement, Object... args) {
		try {
			// assign parameters as genetics (library chooses a type)
			for(int i = 0; i < args.length; i ++) {
				statement.setObject(i + 1, args[i]);
			}
			
			return statement.executeUpdate() != 0;
		} catch(SQLException e) {
			// sneaky throw error, worst case: caught by HTTP thread and respond with 500
			throw ErrorUtil.sneekyThrow(e);
		}
	}
	
//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private Database() { }
}
