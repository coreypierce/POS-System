package edu.wit.se16.system;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunCycle {
	private static final Logger LOG = LoggerFactory.getLogger(RunCycle.class);
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(RunCycle::shutdown, "POS - Shutdown Hook"));
		LOG.info("Shutdown hook added!");
	}

	private static boolean isShutdownStarted = false;
	
	public static interface ShutdownAction { public void shutdown() throws Exception; }
	private static final ArrayList<ShutdownAction> SHUTDOWN_ACTIONS = new ArrayList<>();
	private static final ArrayList<ShutdownAction> PRE_SHUTDOWN_ACTIONS = new ArrayList<>();
	
	public static void addShutdownAction(ShutdownAction action) { SHUTDOWN_ACTIONS.add(action); }
	public static void addPreShutdownAction(ShutdownAction action) { PRE_SHUTDOWN_ACTIONS.add(action); }
	
	public static void addShutdownAction(boolean pre, ShutdownAction action) { 
		if(pre) addPreShutdownAction(action); else addShutdownAction(action); 
	}

	public static void removeShutdownAction(ShutdownAction action) { 
		if(isShutdownStarted) return;
		SHUTDOWN_ACTIONS.remove(action); 
		PRE_SHUTDOWN_ACTIONS.remove(action); 
	}
	
	public static void startShutdown() {
		LOG.info("Starting Shutdown process...");

		for(ShutdownAction action : PRE_SHUTDOWN_ACTIONS) {
			try { action.shutdown(); }
			catch(Exception e) { 
				LOG.error("Uncaught Exception while Shutting-Down", e);
			}
		}

		System.exit(0);
	}
	
	private static void shutdown() {
		LOG.info("Finishing shutdown process...");
		isShutdownStarted = true;

		System.gc();
		
		for(ShutdownAction action : SHUTDOWN_ACTIONS) {
			try { action.shutdown(); }
			catch(Exception e) { 
				LOG.error("Uncaught Exception while Shutting-Down", e);
			}
		}
	}
}
