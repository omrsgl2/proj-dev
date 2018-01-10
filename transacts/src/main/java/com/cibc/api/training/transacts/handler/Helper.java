package com.cibc.api.training.transacts.handler;

import java.util.Deque;

import io.undertow.server.HttpServerExchange;

public class Helper {

	private Helper() {
        throw new AssertionError();
    }

    static int getCustomerId(HttpServerExchange exchange) {
        Deque<String> values = exchange.getQueryParameters().get("customerID");
        if (values == null) {
            return 0;
        }

        String textValue = values.peekFirst();
        if (textValue == null) {
            return 0;
        }

        try {
            int parsedValue = Integer.parseInt(textValue);
            return parsedValue;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

	static String getFirstName(HttpServerExchange exchange) {
        Deque<String> values = exchange.getQueryParameters().get("first-name");
        if (values == null) {
            return null;
        }

        String textValue = values.peekFirst();
        if (textValue == null) {
            return null;
        }

        return textValue;
    }

    static String getLastName(HttpServerExchange exchange) {
        Deque<String> values = exchange.getQueryParameters().get("last-name");
        if (values == null) {
            return null;
        }

        String textValue = values.peekFirst();
        if (textValue == null) {
            return null;
        }

        return textValue;
    }


	static String isNull(String s) {
		if (s == null)
			return "";
		else return s;
	}

}
