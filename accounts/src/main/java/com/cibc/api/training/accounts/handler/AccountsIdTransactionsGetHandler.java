package com.cibc.api.training.accounts.handler;

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
 * Class implements the retrieval of transaction information, for a specified
 * account ID.
 *
 * CIBC Reference Training Materials - API Foundation - 2017
 */
public class AccountsIdTransactionsGetHandler implements HttpHandler {

	static String CONFIG_NAME = "accounts";
	static Logger logger = LoggerFactory.getLogger(AccountsIdTransactionsGetHandler.class);
	
	// create cluster instance for registry
	static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
	
	// host acquired using service discovery
	static String transactsHost;
	// path set in the API's configuration
	static String transactsPath = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("transacts_path");   
	
	// serviceID for downstream API 
	static String transactsServiceID = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("transacts_serviceID");   
			
	// environment set in server.yml
	// downstream API invocation only within the same environment
	static String tag = Server.config.getEnvironment();
	
	static Map<String, Object> securityConfig = (Map<String, Object>) Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);
	static boolean securityEnabled = (Boolean) securityConfig.get(JwtHelper.ENABLE_VERIFY_JWT);
	static Http2Client client = Http2Client.getInstance();
	static ClientConnection connection;

	public AccountsIdTransactionsGetHandler() {
		try {
			// discover the host and establish a connection at API start-up.
			// if downstream API is not up and running, an exception will occur
	        transactsHost = cluster.serviceToUrl("https", transactsServiceID, tag, null);
	        connection = client.connect(new URI(transactsHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
										OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
		} catch (Exception e) {
			logger.error("Exeption:", e);
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		int statusCode = 200;

		// get account id here.
		Integer accountId = Integer.valueOf(exchange.getQueryParameters().get("id").getFirst());

		String transactionString = null;
		// connect if a connection has not already been created
		final CountDownLatch latch = new CountDownLatch(1);
		if (connection == null || !connection.isOpen()) {
			try {
				// discover the host and establish a connection if not established at start-up
				transactsHost = cluster.serviceToUrl("https", transactsServiceID, tag, null);
				connection = client.connect(new URI(transactsHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
											OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
			} catch (Exception e) {
				logger.error("Exeption:", e);
				throw new ClientException(e);
			}
		}

		final AtomicReference<ClientResponse> reference = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath(String.format(transactsPath, accountId));
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
				throw new Exception("Failed to call the transacts: " + statusCode);
			}
			
			// retrieve the response from the transacts API
			transactionString = reference.get().getAttachment(Http2Client.RESPONSE_BODY);

		} catch (Exception e) {
			logger.error("Exception:", e);
			throw new ClientException(e);
		}

		// set the content type in the response
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

		// serialize the response object and set in the response
		exchange.setStatusCode(statusCode);
		exchange.getResponseSender().send(transactionString);
	}
}
