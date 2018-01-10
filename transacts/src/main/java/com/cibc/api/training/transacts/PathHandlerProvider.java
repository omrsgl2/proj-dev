
package com.cibc.api.training.transacts;

import com.cibc.api.training.transacts.handler.TransactionsGetHandler;
import com.cibc.api.training.transacts.handler.TransactionsIdGetHandler;
import com.networknt.health.HealthGetHandler;
import com.networknt.info.ServerInfoGetHandler;
import com.networknt.server.HandlerProvider;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.util.Methods;

public class PathHandlerProvider implements HandlerProvider {
    @Override
    public HttpHandler getHandler() {
        return Handlers.routing()
        
            .add(Methods.GET, "/v1/transactions/{id}", new TransactionsIdGetHandler())
        
            .add(Methods.GET, "/v1/health", new HealthGetHandler())
        
            .add(Methods.GET, "/v1/transactions", new TransactionsGetHandler())
        
            .add(Methods.GET, "/v1/server/info", new ServerInfoGetHandler())
        
        ;
    }
}
