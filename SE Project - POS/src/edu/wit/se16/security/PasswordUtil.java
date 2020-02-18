package edu.wit.se16.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtil {
	private static final String HASH_ALGORITHM = "SHA-256";
	
	private static final MessageDigest DIGESTER; static {
		MessageDigest digest = null;
		try { digest = MessageDigest.getInstance(HASH_ALGORITHM); }
		catch(NoSuchAlgorithmException e) { e.printStackTrace(); }
		DIGESTER = digest;
	}
	
	public static String hash(String password) {
		DIGESTER.update(password.getBytes());
		return new String(DIGESTER.digest()); // This does reset the DIGESTER
	}
	
	public static boolean verifyPassword(String password, String correctHash) {
		return correctHash.equals(hash(password));
	}
	
	private PasswordUtil() { }
}
