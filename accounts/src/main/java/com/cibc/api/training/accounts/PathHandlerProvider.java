
package com.cibc.api.training.accounts;

import com.cibc.api.training.accounts.handler.AccountsGetHandler;
import com.cibc.api.training.accounts.handler.AccountsIdGetHandler;
import com.cibc.api.training.accounts.handler.AccountsIdTransactionsGetHandler;
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
        
            .add(Methods.GET, "/v1/accounts/{id}", new AccountsIdGetHandler())
        
            .add(Methods.GET, "/v1/health", new HealthGetHandler())
        
            .add(Methods.GET, "/v1/accounts", new AccountsGetHandler())
        
            .add(Methods.GET, "/v1/accounts/{id}/transactions", new AccountsIdTransactionsGetHandler())
        
            .add(Methods.GET, "/v1/server/info", new ServerInfoGetHandler())
        
        ;
    }
}
