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
    public void writeToDB(Object object) throws Exception{
        if(object.getClass().isAnnotationPresent(Table.class) == false){
            throw new IllegalArgumentException("This object's class has no mapping in the database");
        }
        Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
        PreparedStatement statement = buildWriteStatement(connection, object);
        System.out.println("Writing to DB...");
        statement.executeUpdate();

        ResultSet resultSet = statement.getGeneratedKeys();
        appendIdAfterCreation(resultSet, object);
        connection.close();
    }

    private PreparedStatement buildWriteStatement(Connection connection, Object object) throws SQLException, IllegalAccessException{
        Table tableAnnotation = object.getClass().getAnnotation(Table.class);
        Field[] fields = object.getClass().getDeclaredFields();
        ArrayList<String> fieldName = new ArrayList<>();
        ArrayList<Object> fieldValue = new ArrayList<>();
        for(Field field: fields){
            if(field.isAnnotationPresent(Column.class)){
                field.setAccessible(true);
                fieldName.add(field.getAnnotation(Column.class).name());
                if (field.isAnnotationPresent(PrimaryKey.class)) {
                    fieldValue.add(null); //primary key are auto incremented in database
                } else {
                    fieldValue.add(field.get(object));
                }
            }
        }
        String sql = "INSERT INTO " + tableAnnotation.name() + " (";
        for(String s:fieldName){
            sql += s + ",";
        }
        sql = sql.substring(0,sql.length()-1);
        sql += ") VALUES (";
        for(String s:fieldName){
            sql += "?,";
        }
        sql = sql.substring(0, sql.length()-1);
        sql += ");";

        PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        for(int i=0; i<fieldValue.size(); i++){
            statement.setObject(i+1, fieldValue.get(i));
        }
        return statement;
    }

    private void appendIdAfterCreation(ResultSet resultSet, Object object) throws SQLException, IllegalAccessException{
        if(resultSet != null){
            if(resultSet.next()) {
                Field[] fields = object.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(PrimaryKey.class)) {
                        field.setAccessible(true);
                        field.set(object, resultSet.getInt(1));
                        break;
                        //currently only work for single generated key for id as primary key, haven't forsee other usage
                    }
                }
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
