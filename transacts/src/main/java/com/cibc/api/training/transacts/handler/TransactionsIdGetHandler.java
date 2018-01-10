package com.cibc.api.training.transacts.handler;

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

import com.cibc.api.training.transacts.model.Transaction;

/**
* Class implements the retrieval of transaction information, as queried by account ID.
*
* CIBC Reference Training Materials - API Foundation - 2017
*/
public class TransactionsIdGetHandler implements HttpHandler {
    // set up the logger
    static final Logger logger = LoggerFactory.getLogger(TransactionsIdGetHandler.class);

    // Access a configured DataSource; retrieve database connections from this DataSource
    private static final DataSource ds = (DataSource) SingletonServiceFactory.getBean(DataSource.class);

    // Get a Jackson JSON Object Mapper, usable for object serialization
    private static final ObjectMapper mapper = Config.getInstance().getMapper();

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
 
        Status status = null;
        int statusCode = 200;
        String resp = null;

        // get transaction id here.
        Integer transactionId = Integer.valueOf(exchange.getQueryParameters().get("id").getFirst());
        Transaction transaction = null;
        try (final Connection connection = ds.getConnection()) {

            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM transaction WHERE id = ?",
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

                statement.setInt(1, transactionId);

                try(ResultSet resultSet = statement.executeQuery()){

                    // extract the transaction data here
                    if (resultSet.next()) {
                        transaction = new Transaction();
                        transaction.setId(Helper.isNull(resultSet.getString("ID")));
                        transaction.setAccountID(Helper.isNull(resultSet.getString("ACCOUNT_ID")));
                        transaction.setTransactionType(Transaction.TransactionTypeEnum.fromValue(Helper.isNull(resultSet.getString("TRANSACTION_TYPE"))));
                        transaction.setAmount(Double.valueOf(Helper.isNull(resultSet.getString("AMOUNT"))));

                        // serialize the response
                        resp = mapper.writeValueAsString(transaction);
                    } else {
                        // transaction data not found
                        status = new Status("ERR12013", transactionId);
                        statusCode = status.getStatusCode();

                        // serialize the error response
                        resp = mapper.writeValueAsString(status);
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

        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        // set the content type in the response
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

        // serialize the response object and set in the response
        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(resp);
    }
}
