package com.cibc.api.training.customers.handler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import com.networknt.client.Http2Client;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.security.JwtHelper;
import com.networknt.server.Server;
import com.networknt.service.SingletonServiceFactory;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

/**
* Class implements the retrieval of account information, for a customer ID.
*
* CIBC Reference Training Materials - API Foundation - 2017
*/
public class CustomersIdAccountsGetHandler implements HttpHandler {
	static String CONFIG_NAME = "customers";
	static Logger logger = LoggerFactory.getLogger(CustomersIdGetHandler.class);
	
	// create cluster instance for registry
	static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
	
	// host acquired using service discovery
	static String accountsHost;
	// path set in the API's configuration
	static String accountsPath = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("accounts_path");   
	
	// serviceID for downstream API 
	static String accountsServiceID = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("accounts_serviceID");   
				
	// environment set in server.yml
	// downstream API invocation only within the same environment
	static String tag = Server.config.getEnvironment();
	
	static Map<String, Object> securityConfig = (Map<String, Object>) Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);
	static boolean securityEnabled = (Boolean) securityConfig.get(JwtHelper.ENABLE_VERIFY_JWT);
	static Http2Client client = Http2Client.getInstance();
	static ClientConnection connection;	
	
    public CustomersIdAccountsGetHandler() {
		try {
			// discover the host and establish a connection at API start-up.
			// if downstream API is not up and running, resolution will occur at runtime
	        accountsHost = cluster.serviceToUrl("https", accountsServiceID, tag, null);
	        connection = client.connect(new URI(accountsHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
										OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
	        		
		} catch (Exception e) {
			logger.error("Exeption:", e);
		}
	}

	@Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        int statusCode = 200;

        // get customer id here.
        Integer customerId = Integer.valueOf(exchange.getQueryParameters().get("id").getFirst());

        String accountString = null;

		// connect if a connection has not already been created
		final CountDownLatch latch = new CountDownLatch(1);
		if (connection == null || !connection.isOpen()) {
			try {
				// discover the host and establish a connection at API runtime
				// if downstream API is not up and running, an exception will occur
		        accountsHost = cluster.serviceToUrl("https", accountsServiceID, tag, null);
		        connection = client.connect(new URI(accountsHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
											OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
			} catch (Exception e) {
				logger.error("Exeption:", e);
				throw new ClientException(e);
			}
		}

		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(String.format(accountsPath, customerId));
			// this is to ask client module to pass through correlationId and traceabilityId
			// as well as
			// getting access token from oauth2 server automatically and attach
			// authorization headers.
			if (securityEnabled)
				client.propagateHeaders(request, exchange);
			
			connection.sendRequest(request, client.createClientCallback(reference, latch));
			latch.await();

			statusCode = reference.get().getResponseCode();

			if (statusCode >= 300) {
				throw new Exception("Failed to call the accounts API: " + statusCode);
			}
			
			// retrieve the response from the accounts API
			accountString = reference.get().getAttachment(Http2Client.RESPONSE_BODY);

		} catch (Exception e) {
			logger.error("Exception:", e);
			throw new ClientException(e);
		}

        // set the content type in the response
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        // serialize the response object and set in the response
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(accountString);
    }
}
