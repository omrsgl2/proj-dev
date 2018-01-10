package com.cibc.api.training.myaccounts.handler;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.client.Http2Client;
import com.networknt.cluster.Cluster;
import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.security.JwtHelper;
import com.networknt.server.Server;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;

import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;

/**
 * MyAccounts implements the retrieval of portfolio information, including
 * customer, account and transaction data based on a customer ID.
 *
 * CIBC Reference Training Materials - API Foundation - 2017
 */
public class MyaccountsIdGetHandler implements HttpHandler {

	static String CONFIG_NAME = "myaccounts";
	static Logger logger = LoggerFactory.getLogger(MyaccountsIdGetHandler.class);

	// create cluster instance for registry
	static Cluster cluster = SingletonServiceFactory.getBean(Cluster.class);
	
	// host and path for the aggregated APIs
	// access customers API
	// host acquired using service discovery
	static String customersHost;
	// path set in the API's configuration
	static String customersPath = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("customers_path");   
	
	// serviceID for downstream API 
	static String customersServiceID = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("customers_serviceID");   
				
	// access accounts API
	// host acquired using service discovery
	static String accountsHost;
	// path set in the API's configuration
	static String accountsPath = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("accounts_path");   
	
	// serviceID for downstream API 
	static String accountsServiceID = (String) Config.getInstance().getJsonMapConfig(CONFIG_NAME).get("accounts_serviceID");   
			
	// access transacts API
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

	static Http2Client clientCustomers = Http2Client.getInstance();
	static Http2Client clientAccounts = Http2Client.getInstance();
	static Http2Client clientTransacts = Http2Client.getInstance();

	static ClientConnection connectionCustomers;
	static ClientConnection connectionAccounts;
	static ClientConnection connectionTransacts;

	// Get a Jackson JSON Object Mapper, usable for object serialization
	private static final ObjectMapper mapper = Config.getInstance().getMapper();

	public MyaccountsIdGetHandler() {
		try {
			// discover the host and establish a connection at API start-up.
			// if downstream API is not up and running, resolution will occur at runtime
			customersHost = cluster.serviceToUrl("https", customersServiceID, tag, null);
			connectionCustomers = clientCustomers.connect(new URI(customersHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
														OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();	
			
	        accountsHost = cluster.serviceToUrl("https", accountsServiceID, tag, null);
	        connectionAccounts = clientAccounts.connect(new URI(accountsHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
														OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();	
	        
	        transactsHost = cluster.serviceToUrl("https", transactsServiceID, tag, null);
	        connectionTransacts = clientTransacts.connect(new URI(transactsHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
										OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
		} catch (Exception e) {
			logger.error("Exception:", e);
		}
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {

		Status status = null;
		int statusCode = 200;

		Map<String, Object> responseMap = new HashMap<String, Object>();

		// get customer id here.
		Integer customerId = Integer.valueOf(exchange.getQueryParameters().get("id").getFirst());

		// -----------------------
		// get the customer data
		// -----------------------
		String customerString = null;

		// connect if a connection has not already been created
		final CountDownLatch latchCustomers = new CountDownLatch(1);
		if (connectionCustomers == null || !connectionCustomers.isOpen()) {
			try {
				// discover the host and establish a connection at API runtime
				// if downstream API is not up and running, an exception will occur
				customersHost = cluster.serviceToUrl("https", customersServiceID, tag, null);
				connectionCustomers = clientCustomers.connect(new URI(customersHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
															OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();	
			} catch (Exception e) {
				logger.error("Exception:", e);
				throw new ClientException(e);
			}
		}

		final AtomicReference<ClientResponse> referenceCustomers = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET)
					.setPath(String.format(customersPath, customerId));
			// this is to ask client module to pass through correlationId and traceabilityId
			// as well as
			// getting access token from oauth2 server automatically and attach
			// authorization headers.
			if (securityEnabled)
				clientCustomers.propagateHeaders(request, exchange);

			connectionCustomers.sendRequest(request,
					clientCustomers.createClientCallback(referenceCustomers, latchCustomers));
			latchCustomers.await();

			statusCode = referenceCustomers.get().getResponseCode();

			if (statusCode >= 300) {
				throw new Exception("Failed to call the customers API: " + statusCode);
			}

			// retrieve the response from the transacts API
			customerString = referenceCustomers.get().getAttachment(Http2Client.RESPONSE_BODY);

		} catch (Exception e) {
			// log the exception
			// logger.error("Exception encountered in the myaccounts API: " +
			// e.getMessage());
			logger.error("Exception encountered in the myaccounts API:", e);

			// customer data not retrieved
			status = new Status("ERR20001", customerId);
			statusCode = status.getStatusCode();

			// serialize the error response
			exchange.getResponseSender().send(mapper.writeValueAsString(status));
			return;
		}

		// get the customer map
		Map<String, Object> customerMap = (Map<String, Object>) mapper.readValue(customerString,
				new TypeReference<Map<String, Object>>() {
				});
		responseMap.putAll(customerMap);

		// get the customer id from the Customer object and use it to retrieve account
		// data by customer id
		Map<String, Object> custMap = (Map<String, Object>) customerMap.get("customers");
		String custId = (String) custMap.get("id");

		// ----------------------
		// get the account data
		// ----------------------
		String accountString = null;

		// connect if a connection has not already been created
		final CountDownLatch latchAccounts = new CountDownLatch(1);
		if (connectionAccounts == null || !connectionAccounts.isOpen()) {
			try {
				// discover the host and establish a connection at API runtime
				// if downstream API is not up and running, an exception will occur
		        accountsHost = cluster.serviceToUrl("https", accountsServiceID, tag, null);
		        connectionAccounts = clientAccounts.connect(new URI(accountsHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
															OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();	
			} catch (Exception e) {
				logger.error("Exception:", e);
				throw new ClientException(e);
			}
		}

		final AtomicReference<ClientResponse> referenceAccounts = new AtomicReference<>();
		try {
			ClientRequest request = new ClientRequest().setMethod(Methods.GET)
					.setPath(String.format(accountsPath, custId));
			// this is to ask client module to pass through correlationId and traceabilityId
			// as well as
			// getting access token from oauth2 server automatically and attach
			// authorization headers.
			if (securityEnabled)
				clientAccounts.propagateHeaders(request, exchange);

			connectionAccounts.sendRequest(request, clientAccounts.createClientCallback(referenceAccounts, latchAccounts));
			latchAccounts.await();

			statusCode = referenceAccounts.get().getResponseCode();

			if (statusCode >= 300) {
				throw new Exception("Failed to call the accounts API: " + statusCode);
			}

			// retrieve the response from the transacts API
			accountString = referenceAccounts.get().getAttachment(Http2Client.RESPONSE_BODY);

		} catch (Exception e) {
			// log the exception
			logger.error("Exception encountered in the myaccounts API: ", e);

			// account data not retrieved
			status = new Status("ERR20002", custId);
			statusCode = status.getStatusCode();

			// serialize the error response
			exchange.getResponseSender().send(mapper.writeValueAsString(status));
			return;
		}

		// extract the account info from the list of accounts
		Map<String, Object> accountMap = mapper.readValue(accountString, new TypeReference<Map<String, Object>>() {
		});

		String transactionString = null;
		List<Map<String, Object>> allAccountsList = new ArrayList<Map<String, Object>>();
		Map<String, Object> oneAccountMap = new HashMap<String, Object>();
		Map<String, Object> transactionMap = null;

		// get the transaction data for all accounts
		List<Map<String, Object>> accountList = (List<Map<String, Object>>) accountMap.get("accounts");
		for (Map<String, Object> account : accountList) {
			// get the account ID
			String accountId = (String) account.get("id");

			// get the transaction data for the account
			final CountDownLatch latchTransacts = new CountDownLatch(1);
			if (connectionTransacts == null || !connectionTransacts.isOpen()) {
				try {
					// discover the host and establish a connection at API runtime
					// if downstream API is not up and running, an exception will occur
			        transactsHost = cluster.serviceToUrl("https", transactsServiceID, tag, null);
			        connectionTransacts = clientTransacts.connect(new URI(transactsHost), Http2Client.WORKER, Http2Client.SSL, Http2Client.POOL,
												OptionMap.create(UndertowOptions.ENABLE_HTTP2, true)).get();
				} catch (Exception e) {
					logger.error("Exception:", e);
					throw new ClientException(e);
				}
			}

			final AtomicReference<ClientResponse> referenceTransacts = new AtomicReference<>();
			try {
				ClientRequest request = new ClientRequest().setMethod(Methods.GET)
						.setPath(String.format(transactsPath, accountId));
				// this is to ask client module to pass through correlationId and traceabilityId
				// as well as
				// getting access token from oauth2 server automatically and attach
				// authorization headers.
				if (securityEnabled)
					clientTransacts.propagateHeaders(request, exchange);

				connectionTransacts.sendRequest(request, clientTransacts.createClientCallback(referenceTransacts, latchTransacts));
				latchTransacts.await();

				statusCode = referenceTransacts.get().getResponseCode();

				if (statusCode >= 300) {
					throw new Exception("Failed to call the transacts: " + statusCode);
				}

				// retrieve the response from the transacts API
				transactionString = referenceTransacts.get().getAttachment(Http2Client.RESPONSE_BODY);

			} catch (Exception e) {
				// log the exception
				logger.error("Exception encountered in the myaccounts API: ", e);

				// transaction data not retrieved
				status = new Status("ERR20003", accountId);
				statusCode = status.getStatusCode();

				// serialize the error response
				exchange.getResponseSender().send(mapper.writeValueAsString(status));
				return;
			}

			// aggregate account data and transactions for one account
			oneAccountMap.put("account", account);

			transactionMap = mapper.readValue(transactionString, new TypeReference<Map<String, Object>>() {
			});
			oneAccountMap.put("transactions", transactionMap);

			// add to the list of accounts
			allAccountsList.add(oneAccountMap);
		}

		// add the accounts list to the response
		responseMap.put("accounts", allAccountsList);

		// set the content type in the response
		exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

		// serialize the response object and set in the response
		exchange.setStatusCode(statusCode);
		exchange.getResponseSender().send(mapper.writeValueAsString(responseMap));
	}
}
