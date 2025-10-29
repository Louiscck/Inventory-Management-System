package org.example;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ItemHandler implements HttpHandler {
    private Database database;
    private HttpResponseHandler responseHandler;
    ItemHandler(Database database){
        this.database = database;
    }

    @Override
    public void handle(HttpExchange exchange){
        try {
            this.responseHandler = new HttpResponseHandler(exchange);
            String method = exchange.getRequestMethod();
            System.out.println("Received HTTP request: " + method);
            if(method.equals("OPTIONS")){
                this.responseHandler.handleCors();
                return;
            }
            if(!exchange.getRequestURI().getPath().split("/")[1].equals("item")){
                this.responseHandler.sendNotFound();
                return;
            }
            switch (method) {
                case "POST":
                    createItem(exchange);
                    break;
                case "GET":
                    handleGetRequestRouting(exchange);
                    //currently only possible to route to getItem() for single keyword search, may implement multiple keyword search in future
                    break;
                case "DELETE":
                    deleteItem(exchange);
                    break;
                case "PUT":
                    replaceItem(exchange);
                    break;
            }
        } catch (Exception e){
            e.printStackTrace();
            this.responseHandler.sendInternalServerError();
        }
    }

    private void handleGetRequestRouting(HttpExchange exchange) throws Exception{
        String query = URLDecoder.decode(exchange.getRequestURI().getQuery(), StandardCharsets.UTF_8); //e.g. name=Relay&category=Electric
        String[] queryArray = query.split("&"); //e.g. {name=Relay, category=Electric}
        HashMap<String, String> queryMap = new HashMap<>(); //e.g. map of {key=name, value=Relay}, {key=category, value=Electric}
        for(String s:queryArray){
            String[] pair = s.split("=");
            if(pair.length == 1) { //e.g. "search=", i.e. no search keyword is keyed in from frontend, which means get all result
                queryMap.put(pair[0], "");
            } else {
                queryMap.put(pair[0], pair[1]);
            }
        }
        if(queryMap.size() == 1 && queryMap.containsKey("search")){ //for now, only implement single keyword search {search= x}
            getItem(exchange, queryMap.get("search").trim());
        } else {
            this.responseHandler.sendResponse(400, "Bad request, query not allowed or not matched.");
        }
        //eventhough now only has single keyword search at frontend, using hashmap facilitate expansion for multi keyword search
    }

    private void createItem(HttpExchange exchange) throws Exception{
        InputStream is = exchange.getRequestBody();
        String requestJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if(isBadCreateRequest(requestJson)){
            return;
        }
        Gson gson = new Gson();
        Item item = gson.fromJson(requestJson, Item.class);
        this.database.createResource(item);
        exchange.getResponseHeaders().add("Location", "/item/" + item.getId());
        this.responseHandler.sendResponse(201, null);
    }

    private boolean isBadCreateRequest(String json) throws Exception{
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        JsonNode amountNode = jsonNode.get("amount");
        return isFieldMissing(jsonNode) || !isPositiveNumber(amountNode);
    }

    private boolean isFieldMissing(JsonNode jsonNode){
        String[] requiredField = {"name", "category", "specification", "unit", "amount"};
        for(String field: requiredField){
            if(!jsonNode.hasNonNull(field) || jsonNode.get(field).asText().trim().isEmpty()){
                this.responseHandler.sendResponse(400, "Missing fields. Please fill up the form.");
                return true;
            }
        }
        return false;
    }

    private boolean isPositiveNumber(JsonNode amountNode){
        int amount;
        try{
            if(amountNode.isNumber()){
                amount = amountNode.asInt();
            } else if (amountNode.isTextual()){ //Textual non-number (e.g. "hi") throws NumberFormatException()
                amount = Integer.parseInt(amountNode.asText().trim());
            } else { //special character
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e){
            this.responseHandler.sendResponse(400, "Amount has to be number.");
            return false;
        }

        if (amount < 0){
            this.responseHandler.sendResponse(400, "Amount must be bigger than 0.");
            return false;
        }
        return true;
    }

    private void getItem(HttpExchange exchange, String keyword) throws Exception{
        ArrayList<Item> itemList = this.database.readResource(keyword, Item.class);
        this.responseHandler.sendResponse(200, itemList);
    }

    private void deleteItem(HttpExchange exchange) throws Exception{
        String idString = exchange.getRequestURI().getPath().split("/")[2];
        if(isBadDeleteRequest(idString)){
            return;
        }
        int id = Integer.parseInt(idString);
        boolean isSuccess = this.database.deleteResource(id, Item.class);
        if(isSuccess){
            this.responseHandler.sendResponse(204, null);
        } else {
            this.responseHandler.sendResponse(404, "Item not found in database.");
        }
    }

    private boolean isBadDeleteRequest(String idString){
        return !isPositiveNumber(idString);
    }

    private boolean isPositiveNumber(String num){
        try{
            int id = Integer.parseInt(num);
            if(id <= 0){
                this.responseHandler.sendResponse(400, "Item id must be larger than 0.");
                return false;
            }
        } catch (NumberFormatException e){
            this.responseHandler.sendResponse(400, "Item id must be a number.");
            return false;
        }
        return true;
    }

    private void replaceItem(HttpExchange exchange) throws Exception{
        InputStream is = exchange.getRequestBody();
        String requestJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        String idString = exchange.getRequestURI().getPath().split("/")[2];
        if(isBadReplaceRequest(requestJson, idString)){
            return;
        }
        int id = Integer.parseInt(idString);
        Gson gson = new Gson();
        Item item = gson.fromJson(requestJson, Item.class);
        boolean isSuccess = this.database.replaceResource(item, id);
        if(isSuccess){
            this.responseHandler.sendResponse(204,null);
        } else {
            this.responseHandler.sendResponse(404, "Item not found in database.");
        }
    }

    private boolean isBadReplaceRequest(String json, String idString) throws Exception{
        //request json MAY contain the id, but for REST standard, the id should be in URI path, hence check id in idString instead of json
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(json);
        JsonNode amountNode = jsonNode.get("amount");
        if(!isPositiveNumber(idString)){
            return true;
        }
        return isFieldMissing(jsonNode) || !isPositiveNumber(amountNode);
    }
}
