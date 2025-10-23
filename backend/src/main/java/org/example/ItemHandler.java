package org.example;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ItemHandler implements HttpHandler {
    private Database database;
    ItemHandler(Database database){
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange){
        try {
            String method = exchange.getRequestMethod();
            System.out.println("Received HTTP request: " + method);
            switch (method) {
                case "OPTIONS":
                    handleCors(exchange);
                    break;
                case "POST":
                    String path = exchange.getRequestURI().getPath();
                    String[] paths = path.split("/");
                    if (paths.length == 2 && paths[1].equals("item")) {
                        createItem(exchange);
                    } else {
                        sendNotFound(exchange);
                    }
                    break;
                case "GET":
                    String urlEncoded = exchange.getRequestURI().toString();
                    String url = URLDecoder.decode(urlEncoded, StandardCharsets.UTF_8);
                    String searchParam = url.split("\\?")[1];
                    String[] searchParams = searchParam.split("&");
                    HashMap<String, String> searchMap = new HashMap<>();
                    for(String s:searchParams){
                        searchMap.put(s.split("=")[0],s.split("=")[1]);
                    }
                    if(searchMap.size() == 1 && searchMap.containsKey("search")){
                        getItem(exchange, searchMap.get("search"));
                    }
                    //eventhough now only has single keyword search at frontend, using hashmap facilitate expansion for multi keyword search
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
        ErrorResponse errorResponse = new ErrorResponse(500, "Internal server error.");
        sendResponse(exchange, errorResponse.getStatusCode(), errorResponse);
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
        ErrorResponse errorResponse = new ErrorResponse(404, "Page not found (endpoint not found).");
        sendResponse(exchange, errorResponse.getStatusCode(), errorResponse);
    }

    private void sendResponse(HttpExchange exchange, int code, Object body){
        System.out.println("sending response back to client...");
        Gson gson = new Gson();
        String bodyJson = gson.toJson(body);
        try{
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bodyJson.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(bodyJson.getBytes());
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
            ErrorResponse errorResponse = new ErrorResponse();
            if(!isBadRequest(requestJson, errorResponse)){
                Item item = gson.fromJson(requestJson, Item.class);
                this.database.writeToDB(item);
                errorResponse.setStatusCode(201);
                exchange.getResponseHeaders().add("Location", "/item/" + item.getId());
            }
            sendResponse(exchange, errorResponse.getStatusCode(), errorResponse);
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private boolean isBadRequest(String json, ErrorResponse errorResponse){
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            JsonNode jsonNode = objectMapper.readTree(json);
            String[] requiredField = {"name", "category", "specification", "unit", "amount"};

            for(String field: requiredField){
                if(!jsonNode.hasNonNull(field) || jsonNode.get(field).asText().isEmpty()){
                    errorResponse.setStatusCode(400);
                    errorResponse.setErrorMessage("Missing fields. Please fill up the form.");
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
                errorResponse.setStatusCode(400);
                errorResponse.setErrorMessage("Amount has to be number.");
                return true;
            }

            if (amount < 0){
                errorResponse.setStatusCode(400);
                errorResponse.setErrorMessage("Amount must be bigger than 0.");
                return true;
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    private void getItem(HttpExchange exchange, String keyword){
        ArrayList<Item> itemList = this.database.readFromDB(keyword, Item.class);
        if(itemList == null){
            throw new IllegalArgumentException("The class itself or its fields are not mapped to the DB");
            //make sure to map with annotation
        }
        sendResponse(exchange,200, itemList);
    }
}
