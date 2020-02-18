package edu.wit.se16.networking;

import java.net.InetAddress;
import java.util.ArrayList;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;

import edu.wit.se16.security.EncryptionUtil;
import edu.wit.se16.system.logging.LoggingUtil;
import edu.wit.se16.util.ErrorUtil;

public class RequestServer {
	private static final Logger LOG = LoggingUtil.getLogger();
	
	private boolean configurationError;
	private Server httpServer;
	
	public RequestServer(InetAddress address, int http_port, int https_port) {
		LOG.info("Initializing RequestServer...");
		
		httpServer = new Server();
		httpServer.setHandler(new RequestDistributor());
		
		ArrayList<Connector> connectors = new ArrayList<>();
		configurationError = false;
		
		// ========== Setup HTTP Connector ========== \\
		
		// check if HTTP connector is disabled
		if(http_port <= 0) {
			LOG.info("HTTP connecter disabled");
		
		// check for configuration issues
		} else if(http_port > 65536) {
			LOG.error("HTTP port out of range {} > 65536", http_port);
		
		// setup HTTP connector
		} else {
			LOG.debug("Setting up HTTP connector...");
			
			ServerConnector httpConnector = new ServerConnector(httpServer);
			httpConnector.setHost(address.getHostName());
			httpConnector.setPort(http_port);
			
			connectors.add(httpConnector);
		}

		
		// ========== Setup HTTPS Connector ========== \\

		// check if HTTPS connector is disabled
		if(https_port <= 0) {
			LOG.info("HTTPS connecter disabled");

		// check for configuration issues
		} else if(https_port > 65536) {
			LOG.error("HTTPS port out of range {} > 65536", http_port);

		// setup HTTPS connector
		} else {
			LOG.debug("Setting up HTTPS connector...");
			
			SslContextFactory sslContextFactory = EncryptionUtil.createSSLFactory();
			HttpConfiguration httpsConfig = new HttpConfiguration();
			httpsConfig.setSecurePort(https_port);
			httpsConfig.setSecureScheme("https");
			httpsConfig.addCustomizer(new SecureRequestCustomizer());
			
			ServerConnector sslConnector = new ServerConnector(httpServer, 
					new SslConnectionFactory(sslContextFactory, "http/1.1"), 
					new HttpConnectionFactory(httpsConfig));
			sslConnector.setHost(address.getHostName());
			sslConnector.setPort(https_port);

			connectors.add(sslConnector);
		}
		
		// ========== Bind Connector ========== \\

		if(connectors.size() <= 0) {
			LOG.error("No connectors are enables/setup");
			configurationError = true;
			return;
		}
		
		LOG.trace("Adding connectors to Server...");
		httpServer.setConnectors(connectors.toArray(new Connector[0]));
	}
	
	/**
	 * 	Starts up RequestServer
	 * 
	 * 	@see Server#start() 
	 */
	public void start() {
		LOG.info("Starting RequestServer...");
		
		// check to make sure the server has a valid configuration
		if(configurationError) {
			LOG.error("Cannot start RequestServer due to configuration error");
			return;
		}
		
		try { 
			httpServer.start();
			
		} catch(Exception e) { 
			LOG.error("An error occured while Starting the RequestServer!", e);
			throw ErrorUtil.sneekyThrow(e);
		}
	}
	
	/**
	 * 	Shuts down RequestServer
	 * 
	 * 	@see Server#stop() 
	 */
	public void stop() {
		LOG.info("Stoping RequestServer...");
		
		try { 
			httpServer.stop(); 
			
		} catch(Exception e) { 
			LOG.error("An error occured while Stopping the RequestServer!", e);
			throw ErrorUtil.sneekyThrow(e);
		}
	}
}
