package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

public class Main {
    public static void main(String[] args) throws IOException {
        String url = "", user = "", password = "";
        try(InputStream is = Main.class.getResourceAsStream("/config.properties")){
            if(is != null){ //for local testing
                Properties properties = new Properties();
                properties.load(is);
                url = properties.getProperty("db.url");
                user = properties.getProperty("db.user");
                password = properties.getProperty("db.password");
                System.out.println("Using local DB");
            } else { //for deployment, reads from Render's environment variables
                String host = System.getenv("DB_HOST");
                String port = System.getenv("DB_PORT");
                String dbName   = System.getenv("DB_NAME");
                url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true";
                user = System.getenv("DB_USER");
                password = System.getenv("DB_PASSWORD");
                System.out.println("Using cloud DB");
            }
        } catch(IOException e){
            e.printStackTrace();
            System.out.println("Error loading config.properties.");
            return;
        }

        if(url.isEmpty() || url == null || user.isEmpty() || user == null || password.isEmpty() || password == null){
            System.out.println("Cannot get database credentials.");
            return;
        }

        Database database = new Database(url, user, password);

        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        System.out.println("Port = " + port);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/item", new ItemHandler(database));
        server.createContext("/", new UnknownHandler());
        server.start();
    }
}