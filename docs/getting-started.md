# Getting started

This document will detail how to get started.

## Prerequisites
- Java 17
- Maven 3

## Dependencies
- MySQL 8.0
- Assertions

#### MySQL 8.0
Install and configure a MySQL database [as described here](configuration-guide.md).

#### Assertions
Install and configure the snomed-release-validation-assertions repository [as described here](importing-assertions.md).

## Running
Once the prerequisites & dependencies have been configured, simply build & run the application.

#### From source code
```bash
mvn clean package
```

```bash
java -Xms1024m -Xmx8g -Daws.region=us-east-1 -jar target/release-validation-framework*.jar --server.port=8081 --server.servlet.context-path=/api
```

#### From Docker
```bash
docker-compose up -d
```