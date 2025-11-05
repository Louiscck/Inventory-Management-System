package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PingHandler implements HttpHandler {
    private HttpResponseHandler responseHandler;
    @Override
    public void handle(HttpExchange exchange){
        this.responseHandler = new HttpResponseHandler(exchange);
        this.responseHandler.sendResponse(204, null);
    }
}
