package org.example;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class UnknownHandler implements HttpHandler {
    private HttpResponseHandler responseHandler;
    @Override
    public void handle(HttpExchange exchange){
        this.responseHandler = new HttpResponseHandler(exchange);
        if(exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")){
            this.responseHandler.handleCors();
        } else {
            this.responseHandler.sendResponse(404,"Page not found (endpoint doesn't exist).");
        }
    }
}
