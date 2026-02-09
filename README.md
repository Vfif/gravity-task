Currency Exchange Rates Provider Service
The Currency Exchange Rates Provider Service aims to deliver up-to-date exchange rates for a list of supported currencies. By default, the currency list is empty but can be extended using existing API endpoints (see API Documentation below). Exchange rates for the supported currencies are updated when the service starts up and then every hour using the fixer.io and other clients.
 
Prerequisites
Java 21 (25 after 16th September)
Maven
Docker
DBeaver or any other database tool in case of IntelliJ IDEA Community Edition
 
API Documentation
GET http://localhost:8080/api/v1/currencies (Get a list of currencies used in the project. Available to everyone)
POST http://localhost:8080/api/v1/currencies?currency=USD (Add a new currency for getting exchange rates. Available to ADMIN only)
GET http://localhost:8080/api/v1/currencies/exchange-rates?amount=15&from=USD&to=EUR (Get exchange rates for a specified currency. Available to everyone)
POST http://localhost:8080/api/v1/currencies/refresh (Get the latest exchange rates. Available to ADMIN only)
GET http://localhost:8080/api/v1/currencies/trends?from=USD&to=EUR&period=12H (Get how much the exchange rate has changed in percentages in the specified period. Period examples: 12H minimal period, 10D, 3M, 1Y. Available only to ADMIN and PREMIUM_USER)
 
Requirements
Create a Spring Boot application with Java version 21, use Maven for building the project. Implement REST API where a customer can execute the actions listed in the “API Documentation” section.
 
Initially the currency list is empty. You should receive exchange rates from external public sources implementing the integration with them. It can be any publicly available source, e.g. fixer.io, exchangeratesapi.io, openexchangerates.org, currencylayer.com, etc. Implement the integration with at least 2 of them.
 
Additionally, create at least 2 standalone services that will be running in your Docker network and will simulate currency exchange rates providers. They should return random exchange rates each time they are requested. You can just hardcode a list of such rates or create a randomizer that will generate them.
 
Receiving exchange rates should be scheduled (e.g. every hour). They should be stored in the database and in a memory Map for quick access (Add Redis*). API gets data from the Map in which only the best up to date rates are stored. The database table for the exchange rates received should contain next columns: ID, base_currency, target_currency, rate, timestamp.
 
For a database use PostgreSQL, manage DB schema with Liquibase (any format - XML, SQL, YAML, JSON) and all changes to its structure should be made via migrations. Data management should be provided with Spring Data JPA.
 
Main application, mock exchange services and PostgreSQL database should be started in Docker containers. Docker containers should be described with docker-compose file.
 
Models, controllers and services should be covered by unit and functional/integration tests, preferably JUnit 5, Spring Test Framework and TestContainers.
 
Use WireMocks to validate requests to the endpoints in functional/integration tests.
 
The project should have API documentation (preferably Swagger/OpenAPI Specification - dynamic or static).
 
Implement exception handling logic using @RestControllerAdvice. Handle them appropriately to return corresponding error JSON with the correct status code. cover expected exception scenarios in functional/integration tests.
 
Add validation annotations (e.g. @NotEmpty) and cover controller validation with tests using @WebMvcTest.
 
Configure linters and static code analyzers (e.g., Jacoco, CheckStyle, PMD). (PiTest*)
 
Add Spring Security to the project. There should be a login page, users and roles. The next users should be created with different permissions to different endpoints specified in the “API documentation” section: USER, PREMIUM_USER, ADMIN. Users and roles should be stored in the database with encrypted passwords. One user can have many roles.
 
Using Optional, StreamAPI and Lombok is required. 
