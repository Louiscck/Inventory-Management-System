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
    public void createResource(Object object) throws Exception{
        if(object.getClass().isAnnotationPresent(Table.class) == false){
            throw new IllegalArgumentException("This object's class has no mapping in the database");
        }
        Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
        PreparedStatement statement = buildCreateStatement(connection, object);
        System.out.println("Creating resource in database...");
        statement.executeUpdate();

        ResultSet resultSet = statement.getGeneratedKeys();
        appendIdAfterCreation(resultSet, object);
        connection.close();
    }

    private PreparedStatement buildCreateStatement(Connection connection, Object object) throws SQLException, IllegalAccessException{
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
                    if(field.getType() == String.class){
                        fieldValue.add(field.get(object).toString().trim());
                    } else {
                        fieldValue.add(field.get(object));
                    }
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
        if(resultSet == null) {
            return;
        }
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

    public <T> ArrayList<T> readResource(String keyword, Class<T> classType) throws Exception{
        if(classType.isAnnotationPresent(Table.class) == false) {
            throw new IllegalArgumentException("The class is not mapped to a table in the database");
        }
        Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
        PreparedStatement readStatement = buildReadStatement(connection, keyword, classType);
        System.out.println("Reading resource from database...");
        ResultSet resultSet = readStatement.executeQuery();
        ArrayList<T> list = convertToListOfClass(resultSet, classType);
        return list;
    }

    private <T> PreparedStatement buildReadStatement(Connection connection, String keyword, Class<T> classType) throws SQLException{
        //search database based on contains(keyword) on EVERY annotated column
        Table tableAnnotation = classType.getAnnotation(Table.class);
        Field[] fields = classType.getDeclaredFields();
        String sql = "SELECT * FROM " + tableAnnotation.name() + " WHERE ";
        int annotatedColumn = 0;
        for(int i=0; i<fields.length; i++){
            if(fields[i].isAnnotationPresent(Column.class) == true){
                annotatedColumn ++;
                sql += fields[i].getAnnotation(Column.class).name() + " LIKE ? OR ";
            }
        }
        if(annotatedColumn == 0){
            throw new IllegalArgumentException("The class don't have any field that is mapped to the database.");
            //may seem redundant with classType.isAnnotationPresent, double protection in case a class is annotated with Table but not Column (which is not logic)
        }
        sql = sql.substring(0, sql.length()-4);
        sql += ";";
        PreparedStatement statement = connection.prepareStatement(sql);
        for(int i=0; i<annotatedColumn; i++){
            statement.setObject(i+1, "%" + keyword + "%");
        }
        return statement;
    }

    private <T> ArrayList<T> convertToListOfClass(ResultSet resultSet, Class<T> classType) throws Exception{
        ArrayList<T> list = new ArrayList<>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        Field[] fields = classType.getDeclaredFields();
        int columnCount = metaData.getColumnCount();
        while(resultSet.next()){
            T object = classType.getDeclaredConstructor().newInstance(); //create object of class <T>
            Map<String, Object> pairs = new HashMap<>(); //key = column name AS IN DATABASE
            for(int i=1; i<=columnCount; i++){
                pairs.put(metaData.getColumnName(i), resultSet.getObject(i));
            }
            for(Field field:fields){
                if(field.isAnnotationPresent(Column.class) == true){
                    String key = field.getAnnotation(Column.class).name();
                    if(pairs.containsKey(key)){
                        Object value = pairs.get(key);
                        field.setAccessible(true);
                        field.set(object, value);
                    } else {
                        System.out.println("Field not found in/not mapped to database: " + key);
                        //should never reach here as SQLSyntaxErrorException will occur first when querying the database with a non-existing column name
                    }
                }
            }
            list.add(object);
        }
        return list;
    }

    public <T> boolean deleteResource(int id, Class<T> classType) throws SQLException{
        if(classType.isAnnotationPresent(Table.class) == false) {
            throw new IllegalArgumentException("The class is not mapped to a table in the database");
        }
        boolean isDeleteSuccess = true;
        Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
        PreparedStatement deleteStatement = buildDeleteStatement(connection, id, classType);
        System.out.println("Deleting resource in database...");
        int affectedRow = deleteStatement.executeUpdate();
        if(affectedRow == 0){
            isDeleteSuccess = false;
        }
        return isDeleteSuccess;
    }

    private <T> PreparedStatement buildDeleteStatement(Connection connection, int id, Class<T> classType) throws SQLException{
        Table tableAnnotation = classType.getAnnotation(Table.class);
        String sql = "DELETE FROM " + tableAnnotation.name() + " WHERE id = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setObject(1, id);
        return statement;
    }

    public boolean replaceResource(Object object, int id) throws Exception{
        if(object.getClass().isAnnotationPresent(Table.class) == false){
            throw new IllegalArgumentException("This object's class has no mapping in the database");
        }
        boolean isUpdateSuccess = true;
        Connection connection = DriverManager.getConnection(this.url, this.user, this.password);
        PreparedStatement deleteStatement = buildReplaceStatement(connection, object, id);
        System.out.println("Replacing resource in database...");
        int affectedRow = deleteStatement.executeUpdate();
        if(affectedRow == 0){
            isUpdateSuccess = false;
        }
        return isUpdateSuccess;
    }

    private PreparedStatement buildReplaceStatement(Connection connection, Object object, int id) throws Exception{
        Table tableAnnotation = object.getClass().getAnnotation(Table.class);
        Field[] fields = object.getClass().getDeclaredFields();
        ArrayList<String> fieldName = new ArrayList<>();
        ArrayList<Object> fieldValue = new ArrayList<>();
        for(Field field: fields){
            if(field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(PrimaryKey.class)){ //check PK to ignore item id
                field.setAccessible(true);
                fieldName.add(field.getAnnotation(Column.class).name());
                if(field.getType() == String.class){
                    fieldValue.add(field.get(object).toString().trim());
                } else {
                    fieldValue.add(field.get(object));
                }
            }
        }

        String url = "UPDATE " + tableAnnotation.name() + " SET ";
        for(String name: fieldName){
            url += name + "=?, ";
        }
        url = url.substring(0, url.length()-2);
        url += " WHERE id = ?;";

        PreparedStatement statement = connection.prepareStatement(url);
        for(int i=0; i<fieldValue.size(); i++){
            statement.setObject(i+1, fieldValue.get(i));
        }
        statement.setObject(fieldValue.size()+1, id);
        return statement;
    }
}
