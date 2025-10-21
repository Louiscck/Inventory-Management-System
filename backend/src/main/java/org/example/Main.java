package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws IOException {
        Properties properties = new Properties();
        try(InputStream is = Main.class.getResourceAsStream("/config.properties")){
            if(is == null){
                System.out.println("config file not found.");
                return;
            }
            properties.load(is);
        }
        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");
        Database database = new Database(url, user, password);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/item", new ItemHandler(database));
        server.createContext("/", new UnknownHandler());
        server.start();
    }
}