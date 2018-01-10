package com.cibc.api.training.accounts.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.service.SingletonServiceFactory;

import com.cibc.api.training.accounts.model.Account;

/**
* Class implements the retrieval of account information, based on customer ID.
*
* CIBC Reference Training Materials - API Foundation - 2017
*/
public class AccountsIdGetHandler implements HttpHandler {
    // set up the logger
    static final Logger logger = LoggerFactory.getLogger(AccountsIdGetHandler.class);

    // Access a configured DataSource; retrieve database connections from this DataSource
    private static final DataSource ds = (DataSource) SingletonServiceFactory.getBean(DataSource.class);

    // Get a Jackson JSON Object Mapper, usable for object serialization
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        Status status = null;
        int statusCode = 200;
        String resp = null;

        // get account id here.
        Integer accountId = Integer.valueOf(exchange.getQueryParameters().get("id").getFirst());

        Account account = null;
        try (final Connection connection = ds.getConnection()) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM account WHERE id = ?",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

                statement.setInt(1, accountId);

                try(ResultSet resultSet = statement.executeQuery()){

                    // extract the account data
                    if (resultSet.next()) {
                        account = new Account();
                        account.setId(Helper.isNull(resultSet.getString("ID")));
                        account.setCustomerID(Helper.isNull(resultSet.getString("CUSTOMER_ID")));
                        account.setAccountType(Account.AccountTypeEnum.fromValue((Helper.isNull(resultSet.getString("ACCOUNT_TYPE")))));
                        account.setBalance(Double.valueOf(Helper.isNull(resultSet.getString("BALANCE"))));

                        // serialize the response
                        resp = mapper.writeValueAsString(account);
                    } else {
                        // account data not found
                        status = new Status("ERR12013", accountId);
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

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        // serialize the response object and set in the response
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(resp);
    }
}
