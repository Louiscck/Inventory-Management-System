package org.example;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Database {
    private String url;
    private String user;
    private String password;

    public Database (String url, String user, String password){
        this.url = url;
        this.user = user;
        this.password = password;
    }

    //just ensure the class and fields are annotated with the correct names according to the DB, reflection + annotation handles the mapping
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

    public <T> ArrayList<T> readFromDB(String keyword, Class<T> classType){
        if(classType.isAnnotationPresent(Table.class) == false) {
            return null;
        }
        Table annotation = classType.getAnnotation(Table.class);
        Field[] fields = classType.getDeclaredFields();
        String sql = "SELECT * FROM " + annotation.name() + " WHERE ";
        boolean hasAtLeastOneColumn = false;
        for(int i=0; i<fields.length; i++){
            if(fields[i].isAnnotationPresent(Column.class) == true){
                hasAtLeastOneColumn = true;
                sql += fields[i].getAnnotation(Column.class).name() + " LIKE '%" + keyword + "%' OR ";
            }
        }
        if(hasAtLeastOneColumn == false){
            return null;
            //may seem redundant with classType.isAnnotationPresent, double protection in case a class is annotated with Table but not Column (which is not logic)
        }
        sql = sql.substring(0, sql.length()-4);
        sql += ";";
        System.out.println(sql);
        ArrayList<T> list = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
            PreparedStatement readStatement = connection.prepareStatement(sql);
            System.out.println("Reading from DB...");
            ResultSet resultSet = readStatement.executeQuery();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while(resultSet.next()){
                T object = classType.getDeclaredConstructor().newInstance(); //create object of class <T>
                Map<String, Object> pairs = new HashMap<>();
                for(int i=1; i<=columnCount; i++){
                    pairs.put(metaData.getColumnName(i), resultSet.getObject(i));
                }
                for(Field field:fields){
                    if(field.isAnnotationPresent(Column.class) == true){
                        Object value = pairs.get(field.getAnnotation(Column.class).name());
                        field.setAccessible(true);
                        field.set(object, value);
                    }
                }
                list.add(object);
            }
        } catch (SQLException e){
            e.printStackTrace();
        } finally {
            return list;
        }
    }
}
