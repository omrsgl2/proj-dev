package com.cibc.api.training.accounts.handler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cibc.api.training.accounts.model.Account;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

/**
* Class implements the retrieval of account information, as queried by customer ID.
*
* CIBC Reference Training Materials - API Foundation - 2017
*/
public class AccountsGetHandler implements HttpHandler {
    // set up the logger
    static final Logger logger = LoggerFactory.getLogger(AccountsGetHandler.class);

    // Access a configured DataSource; retrieve database connections from this DataSource
    private static final DataSource ds = (DataSource) SingletonServiceFactory.getBean(DataSource.class);

    // Get a Jackson JSON Object Mapper, usable for object serialization
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        // get the cust_id from the query parameter
        Integer customerId = Integer.valueOf(exchange.getQueryParameters().get("cust_id").getLast());

        Status status = null;
        int statusCode = 200;
        String resp = null;

        Account account = null;
        try (final Connection connection = ds.getConnection()) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM account WHERE CUSTOMER_ID = ?",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

                statement.setInt(1, customerId);

                try(ResultSet resultSet = statement.executeQuery()){

                    HashMap<String,List> map = new HashMap<String,List>();
                    ArrayList<Account> list = new ArrayList<Account>();

                    // extract the transaction data here
                    while(resultSet.next()) {
                        account = new Account();
                        account.setId(Helper.isNull(resultSet.getString("ID")));
                        account.setCustomerID(Helper.isNull(resultSet.getString("CUSTOMER_ID")));
                        account.setAccountType(Account.AccountTypeEnum.fromValue((Helper.isNull(resultSet.getString("ACCOUNT_TYPE")))));
                        account.setBalance(Double.valueOf(Helper.isNull(resultSet.getString("BALANCE"))));

                        // add the account to the list
                        list.add(account);
                    }

                    // check if account data has been found
                    if(list.isEmpty()) {
                        status = new Status("ERR12014", String.format("data for customer with ID:%d", customerId));
                        statusCode = status.getStatusCode();

                        // serialize the error response
                        resp = mapper.writeValueAsString(status);
                    } else {
                        // add the list of accounts to the map
                        map.put("accounts", list);

                        // serialize the response
                        resp = mapper.writeValueAsString(map);
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

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        // serialize the response object and set in the response
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(resp);
    }
}
