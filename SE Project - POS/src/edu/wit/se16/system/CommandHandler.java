package edu.wit.se16.system;

import java.io.IOException;

import org.slf4j.Logger;

import edu.wit.se16.system.logging.LoggingUtil;

public class CommandHandler {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	public CommandHandler() {
		Thread thread = new Thread(this::run, "Server Command - Thread");
		thread.start();
	}
	
	private void handleCommand(String cmd) {
		switch(cmd.toLowerCase()) {
			case "exit":
			case "stop":
			case "shutdown":
				LOG.info("Shutting Down System...");
				RunCycle.startShutdown();
				return;
				
			default:
				LOG.warn("unknown command: {}", cmd);
				return;
		}
	}
	
	private void run() {
		try {
			int read;
			byte[] buffer = new byte[512];
			
			while((read = System.in.read(buffer)) > 0) {
				handleCommand(new String(buffer, 0, read));
			}
		} catch(IOException e) {
			// most likely stream-closed
			LOG.info("input-stream has closed");
		}
	}
}
