package edu.wit.se16.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import edu.wit.se16.util.ErrorUtil;

public class PasswordUtil {
	public static final int KEY_SIZE = 512;
	public static final int SALT_SIZE = 64;
	public static final int ITERATIONS = 1024;
	
	private static final SecureRandom RANDOM_GENERATOR = ErrorUtil.sneak(SecureRandom::getInstanceStrong);
	
	public static class Password {
		public String hash;
		public String salt;
		
		public String plain_text;
	}
	
	public static Password generatePassword() {
		return generatePassword(createTempPassword());
	}
	
	public static Password generatePassword(String password) {
		Password result = new Password();
		result.plain_text = password;
		
		byte[] salt = generateSalt();
		byte[] hash = hash(password, salt);
		
		result.salt = encode(salt);
		result.hash = encode(hash);
		return result;
	}
	
	/**
	 * 	Generates a 6-character random password, of lower-case letters
	 */
	private static String createTempPassword() {
		byte[] bytes = new byte[6];
		RANDOM_GENERATOR.nextBytes(bytes);
		
		int range = 'z' - 'a';
		
		StringBuilder password = new StringBuilder(6);
		for(int i = 0; i < bytes.length; i ++) {
			password.append((char) (bytes[i] % range + 'a'));
		}
		
		return password.toString();
	}
	
	public static String generateValidationHash(String password, String salt, int iterations, int keysize) {
		PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
		generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
				password.toCharArray()), decode(salt), iterations); 
		
		KeyParameter parameters = (KeyParameter) generator.generateDerivedParameters(keysize);
		return encode(parameters.getKey());
	}
	
	private static byte[] hash(String password, byte[] salt) {
		// PBKDF2-SHA256 via SKCS5S2 generator
		PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
		generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(
				password.toCharArray()), salt, ITERATIONS);
		
		KeyParameter parameters = (KeyParameter) generator.generateDerivedParameters(KEY_SIZE);
		return parameters.getKey();
	}
	
	private static byte[] generateSalt() {
		byte[] salt = new byte[SALT_SIZE];
		RANDOM_GENERATOR.nextBytes(salt);
		return salt;
	}
	
	public static String generateFakeSalt(long seed) {
		Random rand = new Random(seed);
		byte[] salt = new byte[SALT_SIZE];
		rand.nextBytes(salt);
		
		return encode(salt);
	}
	
	private static String encode(byte[] data) {
		return Base64.getEncoder().encodeToString(data);
	} 

	private static byte[] decode(String data) {
		return Base64.getDecoder().decode(data);
	} 

//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private PasswordUtil() { }
}
