package org.example;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.event.HyperlinkEvent;

public class ItemHandler implements HttpHandler {
    private Database database;
    ItemHandler(Database database){
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange){
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String[] paths = path.split("/");
            System.out.println("Received HTTP request: " + method);
            for (String p : paths) {
                System.out.println("part = " + p);
            }
            switch (method) {
                case "OPTIONS":
                    handleCors(exchange);
                    break;
                case "POST":
                    if (paths.length == 2 && paths[1].equals("item")) {
                        createItem(exchange);
                    } else {
                        sendNotFound(exchange);
                    }
                    break;
                case "GET":
                    getItem(exchange);
                    break;
            }
        } catch (Exception e){
            e.printStackTrace();
            sendInternalServerError(exchange);
        } finally {
            exchange.close();
        }
    }

    private void sendInternalServerError(HttpExchange exchange){
        Response response = new Response(500, "Internal server error.");
        sendResponse(exchange, response);
    }

    private void handleCors(HttpExchange exchange){
        try{
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1); // No response body
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void sendNotFound(HttpExchange exchange){
        Response response = new Response(404, "Page not found (endpoint not found).");
        sendResponse(exchange, response);
    }

    private void sendResponse(HttpExchange exchange, Response response){
        Gson gson = new Gson();
        String responseJson = gson.toJson(response);
        try{
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.getStatusCode(), responseJson.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseJson.getBytes());
            os.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void createItem(HttpExchange exchange){
        InputStream is = exchange.getRequestBody();
        try {
            String requestJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            Response response = new Response();
            if(!isBadRequest(requestJson, response)){
                Item item = gson.fromJson(requestJson, Item.class);
                this.database.writeToDB(item);
                response.setStatusCode(201);
                exchange.getResponseHeaders().add("Location", "/item/" + item.getId());
            }
            sendResponse(exchange, response);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private boolean isBadRequest(String json, Response response){
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            JsonNode jsonNode = objectMapper.readTree(json);
            String[] requiredField = {"name", "category", "specification", "unit", "amount"};

            for(String field: requiredField){
                if(!jsonNode.hasNonNull(field) || jsonNode.get(field).asText().isEmpty()){
                    response.setStatusCode(400);
                    response.setErrorMessage("Missing fields. Please fill up the form.");
                    return true;
                }
            }

            JsonNode amountNode = jsonNode.get("amount");
            int amount;
            try{
                if(amountNode.isNumber()){
                    amount = amountNode.asInt();
                } else if (amountNode.isTextual()){
                    amount = Integer.parseInt(amountNode.asText());
                } else {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e){
                response.setStatusCode(400);
                response.setErrorMessage("Amount has to be number.");
                return true;
            }

            if (amount < 0){
                response.setStatusCode(400);
                response.setErrorMessage("Amount must be bigger than 0.");
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private void getItem(HttpExchange exchange){

    }
}
