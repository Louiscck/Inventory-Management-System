# Inventory Management System

A simple web app for inventory management, built for practice and portfolio purpose. Main purpose is to learn building from scratch and slowly upgrading with frameworks.

## Tech Stack
- **Frontend:** HTML, CSS, JavaScript, Fetch API
- **Backend:** Java, HttpServer, REST API, JDBC
- **Database:** MySQL
- **IDE/Tool:** VSCode, IntelliJ, MySQL Workbench, GitHub, Postman

## Features
- Basic CRUD operation, can add, edit, delete, and search items for managing an inventory.
- Lightweight custom backend without frameworks.
- Lightweight custom ORM.
- Simple UI built with vanilla HTML, CSS, and JS

## Future Plan
- Upgrade frontend with **React**
- Replace JDBC + custom ORM with **Hibernate**
- Upgrade backend with **Spring Boot**
- Add more features like user authentication and role-based access control.

## How to Run Locally
- Clone the repository
- Create and run a MySQL database, import & run schema.sql (found in /database).
- Create config.properties in /backend/src/main/resources and copy paste the following:
db.url=*insert your url here* (e.g. jdbc:mysql://127.0.0.1:3300/schema_name)
db.user=*insert your DB username here*
db.password=*insert your DB password here*
- You can use any schema name, username and password for your database, just make sure they are tally.
- For the backend server, you can either run it in intelliJ OR run the executable in cmd: java -jar out/artifacts/Inventory_Management_System_jar/Inventory_Management_System.jar
- Open the frontend html file on your browser, double click index.html in /frontend

## Website Link
- Not yet host publicly