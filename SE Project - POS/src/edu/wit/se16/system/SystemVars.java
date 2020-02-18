package edu.wit.se16.system;

public class SystemVars {
	public static final boolean ALLOW_UNSAFE_CLEANUP = true;
	
	public static final String KEYSTORE_FILE = LocalVars.ROOT_FOLDER + "/httpsCertificate";
	public static final String KEYSTORE_PASSWORD = "";
	

//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private SystemVars() { }
}
