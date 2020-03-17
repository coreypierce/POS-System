package edu.wit.se16.system;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import edu.wit.se16.system.logging.LoggingUtil;

public class LocalVars {
// ======================================== File Locations ======================================== \\
	
	/** Folder used to store program data */
	public static final String ROOT_FOLDER = System.getProperty("user.home") + "/se16-POS";
	
// ======================================== Binding Variables ======================================== \\

	/** Internal IP address to bind HTTP-Endpoint Server to */
	public static final InetAddress LOCAL_ADDRESS;
	static {
		InetAddress address = InetAddress.getLoopbackAddress();
		try { address = Inet4Address.getByName(""); } // <-- Your binding address here, or blank for localhost
		catch (UnknownHostException e) { LoggingUtil.getLogger().error("Unknown Host Address"); }
		
		LOCAL_ADDRESS = address;
	}
	
	/** Local Port number to bind HTTP-Connector to */
	public static final int HTTP_PORT = 80;
	/** Local Port number to bind HTTPS-Connector to */
	public static final int HTTPS_PORT = 3001;

	
//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private LocalVars() { }
}
