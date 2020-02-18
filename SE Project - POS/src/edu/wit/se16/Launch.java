package edu.wit.se16;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import edu.wit.se16.networking.RequestServer;
import edu.wit.se16.system.CommandHandler;
import edu.wit.se16.system.LocalVars;
import edu.wit.se16.system.RunCycle;
import edu.wit.se16.system.logging.console.LogConsole;
import edu.wit.se16.util.ErrorUtil;

public class Launch {
	// https://www.favicon-generator.org/
	
	public static void main(String[] args) throws Exception {
		// Must be run first, as it redirects System.in/out/err
		new LogConsole();
		
		Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.info("Initializing System...");
		
		// check if local-storage exists
		File root_folder = new File(LocalVars.ROOT_FOLDER);
		if(!root_folder.exists()) {
			root.info("Creating local data-store...");
			root.trace("Creating folder at {}", LocalVars.ROOT_FOLDER);
			
			// if folder is missing, create directory 
			root_folder.mkdirs();
		}
		
		AtomicBoolean keepAlive = new AtomicBoolean(true);

		RunCycle.addPreShutdownAction(() -> keepAlive.set(false));
		RunCycle.addShutdownAction(() -> root.info("System Shutdown, GoodBye!"));

		// setup and start server
		RequestServer server = new RequestServer(LocalVars.LOCAL_ADDRESS, LocalVars.HTTP_PORT, LocalVars.HTTPS_PORT);
		RunCycle.addPreShutdownAction(() -> server.stop());
		
		try {
			server.start();
			root.info("System Started!");
		} catch(Exception e) {
			root.error("Failed to start server!");
		}
		
		// start command-handler
		new CommandHandler();
		
		while(keepAlive.get()) ErrorUtil.ignore(() -> Thread.sleep(100));
	}
}