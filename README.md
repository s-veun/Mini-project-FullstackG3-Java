# JDBC E-Commerce Marketplace

Console marketplace built with Java, PostgreSQL and plain JDBC. SQL access is kept in DAO classes; services enforce role/business rules; controllers provide the console workflow.

## Setup

1. Create a PostgreSQL database, then run `database/schema.sql` in that database. This is the initial schema for a new database; it does not migrate an existing schema.
2. Set the connection environment variables:
   ```sh
   export DB_URL=jdbc:postgresql://localhost:5432/your_database
   export DB_USER=your_database_user
   export DB_PASSWORD=your_database_password
   ```
3. Create an initial administrator by registering through the app as a customer and promoting that user with a trusted PostgreSQL connection:
   ```sql
   UPDATE users SET role = 'ADMIN' WHERE username = 'your_username';
   ```
4. Start the CLI with `./gradlew run` or run `org.example.Main` from an IDE.

Registration allows CUSTOMER and SELLER roles; ADMIN accounts cannot self-register. Passwords are stored using salted PBKDF2 hashes.

## Product import

CSV, XLSX and XLS product imports use the first row as headers. Required headers are:

```text
category_id,product_name,description,price,stock_quantity
```

Imports are limited to 1,000 products and are inserted in one database transaction. An import with an invalid product row is rolled back. The active seller imports products under their own account; admins must provide a seller user ID.

## Reports

Admin and seller reports include completed sales, revenue totals and top-performing products. CSV and XLSX exports are saved under `reports/`.

## Build and tests

Run `./gradlew test`. All database operations use JDBC and prepared statements; there is no ORM dependency.
