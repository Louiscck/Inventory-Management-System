package org.example;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class UnknownHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")){
            handleCors(exchange);
        } else {
            Response response = new Response(404,"Page not found (endpoint doesn't exist).");
            Gson gson = new Gson();
            String responseJson = gson.toJson(response);
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.getStatusCode(), responseJson.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseJson.getBytes());
            os.close();
            exchange.close();
        }
    }

    private void handleCors(HttpExchange exchange){
        try{
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1); // No response body
            exchange.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
