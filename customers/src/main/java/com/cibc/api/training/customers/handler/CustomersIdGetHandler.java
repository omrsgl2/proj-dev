package com.cibc.api.training.customers.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.HashMap;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.networknt.security.JwtHelper;
import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.service.SingletonServiceFactory;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cibc.api.training.customers.model.Customer;

/**
* Class implements the retrieval of customer information, as queried by customer ID.
*
* CIBC Reference Training Materials - API Foundation - 2017
*/
public class CustomersIdGetHandler implements HttpHandler {
    // set up the logger
    static final Logger logger = LoggerFactory.getLogger(CustomersIdGetHandler.class);
    static Map<String, Object> securityConfig = (Map)Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);
    static boolean securityEnabled = (Boolean)securityConfig.get(JwtHelper.ENABLE_VERIFY_JWT);

    // Access a configured DataSource; retrieve database connections from this DataSource
    private static final DataSource ds = (DataSource) SingletonServiceFactory.getBean(DataSource.class);

    // Get a Jackson JSON Object Mapper, usable for object serialization
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        Status status = null;
        int statusCode = 200;
        String resp = null;


        // get customer id here.
        Integer customerId = Integer.valueOf(exchange.getQueryParameters().get("id").getFirst());
        Customer customer = null;
        try (final Connection connection = ds.getConnection()) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM customer WHERE id = ?",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

                statement.setInt(1, customerId);

                try(ResultSet resultSet = statement.executeQuery()) {

                    // extract the customer data
                    if (resultSet.next()) {
                        // customer data successfully retrieved
                        customer = new Customer();
                        customer.setId(Helper.isNull(resultSet.getString("ID")));
                        customer.setFirstName(Helper.isNull(resultSet.getString("FIRST_NAME")));
                        customer.setLastName(Helper.isNull(resultSet.getString("LAST_NAME")));
                        customer.setMiddleInitial(Helper.isNull(resultSet.getString("MIDDLE_INITIAL")));

                        // serialize the response
                        Map<String, Customer> map = new HashMap<String, Customer>();
                        map.put("customers", customer);

                        resp = mapper.writeValueAsString(map);
                    } else {
                        // customer data not found
                        status = new Status("ERR12013", customerId);
                        statusCode = status.getStatusCode();

                        // serialize the error response
                        resp = mapper.writeValueAsString(status);
                    }
                }
            }
        } catch (Exception e) {
            // log the exception
            logger.error("Exception encountered in the customers API: ", e);

            // This is a runtime exception
            status = new Status("ERR10010");
            statusCode = status.getStatusCode();

            // serialize the error response
            resp = mapper.writeValueAsString(status);
        }

        // set the content type in the response
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        // serialize the response object and set in the response
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(resp);
    }
}
