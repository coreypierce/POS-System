package edu.wit.se16.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.bouncycastle.operator.OperatorCreationException;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.wit.se16.security.CertificateGenerator.CertificateProperties;
import edu.wit.se16.security.CertificateGenerator.KeyStoreProperties;
import edu.wit.se16.system.SystemVars;

public class EncryptionUtil { 
	private static final Logger LOG = LoggerFactory.getLogger(EncryptionUtil.class);

	// For more information on Distinguished-Names see
	// https://www.cryptosys.net/pki/manpki/pki_distnames.html
	private static final String CERTIFICATE_SUBJECT_DN = "CN=POS,O=SE16,ST=MA,C=US";
	private static final String CERTIFICATE_ALIAS = "jetty";
	
	private final static CertificateProperties CERTIFICATE_PROPERTIES = new CertificateProperties(
			CERTIFICATE_ALIAS, CERTIFICATE_SUBJECT_DN, Duration.of(31, ChronoUnit.DAYS));
	
	private final static KeyStoreProperties KEYSTORE_PROPERTIES = new KeyStoreProperties(
				SystemVars.KEYSTORE_FILE, SystemVars.KEYSTORE_PASSWORD);
	
	
//	private static final String ALGORITUM = "RSA";
//	private static final String SIGNING_ALGORITHUM = "MD5withRSA";
//	private static final String CERTIFICATE_FACTORY_TYPE = "X.509"; // See "http://www.ietf.org/rfc/rfc3280.txt" for Specification Details
	
	public static SslContextFactory createSSLFactory() {
		try {
			CertificateGenerator.generateCerificateIfInvalid(KEYSTORE_PROPERTIES, CERTIFICATE_PROPERTIES);
			
		} catch(IOException | GeneralSecurityException | OperatorCreationException e) {
			LOG.error("An Unexpected Error occured while checking/generating certificate!", e);
			System.exit(1);
		}
		
		SslContextFactory factory = new SslContextFactory();
			factory.setKeyStorePath(KEYSTORE_PROPERTIES.getFullPath());
			factory.setKeyStorePassword(KEYSTORE_PROPERTIES.password);
			factory.setKeyStoreType(KEYSTORE_PROPERTIES.keyStoreType);
			factory.setCertAlias(CERTIFICATE_PROPERTIES.alias);
		
		return factory;
	}
	
//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private EncryptionUtil() { }
}
