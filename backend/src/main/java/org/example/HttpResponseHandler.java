package org.example;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class HttpResponseHandler {

    private HttpExchange exchange;

    public HttpResponseHandler(HttpExchange exchange){
        this.exchange = exchange;
    }

    public void sendInternalServerError(){
        sendResponse(500, "Internal server error.");
    }

    public void handleCors(){
        this.exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        this.exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        sendResponse(204,null);
    }

    public void sendNotFound(){
        sendResponse(404, "Page not found. (Endpoint or resource not found)");
    }

    public void sendResponse(int code, String errorMessage){
        if(errorMessage == null){
            sendResponse(code, (Object) null);
            return;
        }
        Map<String, String> map = Map.of("errorMessage", errorMessage);
        sendResponse(code, map);
    }

    public void sendResponse(int code, Object body){
        //make sure to not call this function twice in a single exchange, make sure to return properly after calling this once.
        System.out.println("Sending response back to client..." + code + " " + body);
        try{
            this.exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if(body == null){
                this.exchange.sendResponseHeaders(code, -1); // No response body
                return;
            }
            Gson gson = new Gson();
            String bodyJson = gson.toJson(body);
            this.exchange.getResponseHeaders().add("Content-Type", "application/json");
            this.exchange.sendResponseHeaders(code, bodyJson.getBytes().length);
            OutputStream os = this.exchange.getResponseBody();
            os.write(bodyJson.getBytes());
            os.close();
        } catch (IOException e){
            e.printStackTrace();
        } finally {
            this.exchange.close();
        }
    }

}
