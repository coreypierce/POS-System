package edu.wit.se16.system;

public class SystemVars {
	public static final boolean ALLOW_UNSAFE_CLEANUP = true;
	
// ======================================== HTTPS Certificate Variables ======================================== \\
	
	public static final String KEYSTORE_FILE = LocalVars.ROOT_FOLDER + "/httpsCertificate";
	public static final String KEYSTORE_PASSWORD = "";

// ======================================== Database Variables ======================================== \\

	public static final String DATABASE_HOSTNAME = "localhost";
	public static final int DATABASE_PORT = 3306;
	
	public static final String DATABASE_USERNAME = "root";
	public static final String DATABASE_PASSWORD = "";
	

//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private SystemVars() { }
}
