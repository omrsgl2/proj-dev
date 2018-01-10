package com.cibc.api.training.transacts.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.service.SingletonServiceFactory;

import com.cibc.api.training.transacts.model.Transaction;

/**
* Class implements the retrieval of transaction information, as queried by a account ID.
*
* CIBC Reference Training Materials - API Foundation - 2017
*/
public class TransactionsGetHandler implements HttpHandler {
    // set up the logger
    static final Logger logger = LoggerFactory.getLogger(TransactionsGetHandler.class);

    // Access a configured DataSource; retrieve database connections from this DataSource
    private static final DataSource ds = (DataSource) SingletonServiceFactory.getBean(DataSource.class);

    // Get a Jackson JSON Object Mapper, usable for object serialization
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
 
        // get the account_id from the query parameter
        Integer accountId = Integer.valueOf(exchange.getQueryParameters().get("account_id").getLast());

        Status status = null;
        int statusCode = 200;
        String resp = null;

        Transaction transaction = null;
        try (final Connection connection = ds.getConnection()) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM transaction WHERE account_id = ?",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

                statement.setInt(1, accountId);

                try(ResultSet resultSet = statement.executeQuery()){

                    HashMap<String,List> map = new HashMap<String,List>();
                    ArrayList<Transaction> list = new ArrayList<Transaction>();


                    // extract the transaction data here
                    while(resultSet.next()) {
                        transaction = new Transaction();
                        transaction.setId(Helper.isNull(resultSet.getString("ID")));
                        transaction.setAccountID(Helper.isNull(resultSet.getString("ACCOUNT_ID")));
                        transaction.setTransactionType(Transaction.TransactionTypeEnum.fromValue(Helper.isNull(resultSet.getString("TRANSACTION_TYPE"))));
                        transaction.setAmount(Double.valueOf(Helper.isNull(resultSet.getString("AMOUNT"))));

                        // add the transaction to the list
                        list.add(transaction);
                    }

                    // check if transaction data has been found
                    if(list.isEmpty()) {
                        // transaction data not found
                        status = new Status("ERR12014", String.format("data for account with ID:%d", accountId));
                        statusCode = status.getStatusCode();

                        // serialize the error response
                        resp = mapper.writeValueAsString(status);
                    } else {
                        // add the list of transactions to the map
                        map.put("transactions", list);

                        // serialize the response
                        resp = mapper.writeValueAsString(map);
                    }
                }
            }
        } catch (Exception e) {
          // log the exception
          logger.error("Exception encountered in the transacts API: ",e);

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
