package org.example;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.*;

public class Database {
    private String url;
    private String user;
    private String password;

    public Database (String url, String user, String password){
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public void writeToDB(Object object) {
        if(object.getClass().isAnnotationPresent(Table.class) == false){
            return;
        } else {
            Table annotation = object.getClass().getAnnotation(Table.class);
            String sql = "INSERT INTO " + annotation.name() + "VALUES (";
            try{
                Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
                PreparedStatement statement = connection.prepareStatement("INSERT INTO items VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                Field[] field = object.getClass().getDeclaredFields();

                for(int i=0; i<field.length; i++){
                    field[i].setAccessible(true);
                    if(field[i].isAnnotationPresent(PrimaryKey.class)){
                        statement.setObject(i+1, null);
                    } else {
                        statement.setObject(i+1, field[i].get(object));
                    }
                }
                System.out.println("Writing to DB...");
                statement.executeUpdate();

                ResultSet resultSet = statement.getGeneratedKeys();
                if(resultSet.next()){
                    if(object instanceof Item){
                        ((Item) object).setId(resultSet.getInt(1));
                        System.out.println("Id of the new item is " + ((Item) object).getId());
                    }
                }

                connection.close();
            } catch (SQLException | IllegalAccessException e){
                e.printStackTrace();
                throw new RuntimeException("Database write failed", e);
            }
        }
    }
}
