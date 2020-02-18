package edu.wit.se16.security;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;

import edu.wit.se16.system.logging.LoggingUtil;

public class CertificateGenerator {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	public static class KeyStoreProperties {
		public String fileName;
		public String password;

		public KeyStoreProperties(String fileName, String password) {
			this.fileName = fileName;
			this.password = password;
		}

		public String keyStoreType = KeyStore.getDefaultType();
		public String getFullPath() { return fileName + "." + keyStoreType; }
	}
	
	public static class CertificateProperties {
		public String subjectDN;				
		public Duration experationDuration;
		
		public String alias;
		
		public CertificateProperties(String alias, String subjectDN, Duration experationDuration) {
			this.subjectDN = subjectDN;
			this.experationDuration = experationDuration;
			this.alias = alias;
		}

		public int keySize = 2048;
		public String keyAlgorithum = "RSA";
		public String keySignatureAlgorithum = "SHA256WithRSA";
		
		// -------------------------------------------------- \\
		
		private KeyPair keyPair;
	}
	
	public static KeyStore createCertificateStore(KeyStoreProperties storeProperties, CertificateProperties certProperties) throws OperatorCreationException, GeneralSecurityException, IOException {
		KeyStore keyStore = KeyStore.getInstance(storeProperties.keyStoreType);
		X509Certificate certificate = generateCertificate(certProperties);
		
		
		keyStore.load(null);
		KeyPair keyPair = certProperties.keyPair;
		keyStore.setKeyEntry(certProperties.alias, keyPair.getPrivate(), storeProperties.password.toCharArray(), new Certificate[] { certificate });
		
		if(storeProperties.fileName != null) {
			try(FileOutputStream output = new FileOutputStream(storeProperties.getFullPath())) {
				keyStore.store(output, storeProperties.password.toCharArray());
			}
		}
		
		return keyStore;
	}

	private static KeyPair generateKeys(CertificateProperties certProperties) throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance(certProperties.keyAlgorithum);
		generator.initialize(certProperties.keySize);
		return certProperties.keyPair = generator.generateKeyPair();
	}
	
	public static X509Certificate generateCertificate(CertificateProperties prop) throws OperatorCreationException, CertIOException, GeneralSecurityException {
		Provider bcProvider = new BouncyCastleProvider();
	    Security.addProvider(bcProvider);

	    Instant now = Instant.now();
	    Date startDate = Date.from(now);
	    Date endDate = Date.from(now.plus(prop.experationDuration));

	    X500Name dnName = new X500Name(prop.subjectDN);
	    BigInteger certSerialNumber = BigInteger.valueOf(now.toEpochMilli()); // <-- Using the current timestamp as the certificate serial number


	    KeyPair keyPair = generateKeys(prop);
	    ContentSigner contentSigner = new JcaContentSignerBuilder(prop.keySignatureAlgorithum).build(keyPair.getPrivate());
	    JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(dnName, certSerialNumber, startDate, endDate, dnName, keyPair.getPublic());

	    // Extensions --------------------------

	    // Basic Constraints
	    BasicConstraints basicConstraints = new BasicConstraints(true); // <-- true for CA, false for EndEntity
	    certBuilder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, basicConstraints); // Basic Constraints is usually marked as critical.

	    // -------------------------------------

	    return new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(contentSigner));
	}
	
	public static void generateCerificateIfInvalid(KeyStoreProperties storeProperties, CertificateProperties certProperties) throws OperatorCreationException, GeneralSecurityException, IOException {
		try(FileInputStream input = new FileInputStream(storeProperties.getFullPath())) {
			KeyStore keyStore = KeyStore.getInstance(storeProperties.keyStoreType);
			keyStore.load(input, storeProperties.password.toCharArray());
			
			Certificate certificate = keyStore.getCertificate(certProperties.alias);
			if(certificate != null && certificate instanceof X509Certificate) {
				X509Certificate x509Certificate = (X509Certificate) certificate;
				if(Date.from(Instant.now()).before(x509Certificate.getNotAfter())) {
					return; // Certificate is "valid"
				}
				
				LOG.warn("Certificate has Expiered!");
				
			} else {
				LOG.error(certificate == null ? "No Certificate found!" : "Certificate is of the wrong Type!");
			}
			
		} catch(IOException | GeneralSecurityException e) {
			System.err.println(e.getMessage());
		}
		
		createCertificateStore(storeProperties, certProperties);
	}
}
