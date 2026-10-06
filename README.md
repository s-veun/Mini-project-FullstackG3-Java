# JDBC E-Commerce Marketplace

Console marketplace built with Java, PostgreSQL and plain JDBC. SQL access is kept in DAO classes; services enforce role/business rules; controllers provide the console workflow.
Imports are limited to 1,000 products and are inserted in one database transaction. An import with an invalid product row is rolled back. The active seller imports products under their own account; admins must provide a seller user ID.

## Reports

Admin and seller reports include completed sales, revenue totals and top-performing products. CSV and XLSX exports are saved under `reports/`.

## Build and tests

Run `./gradlew test`. All database operations use JDBC and prepared statements; there is no ORM dependency.
