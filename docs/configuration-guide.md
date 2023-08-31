# Configuration Guide
## Overview
RVF uses Spring Boot and you can follow [Spring Boot configuration guide](https://docs.spring.io/spring-boot/docs/2.7.14/reference/htmlsingle/#features.external-config)

## Default Configurations

You can find a list of default configurations in **[/src/main/resources/application.properties](../src/main/resources/application.properties)**.

## MySQL database and user
RVF expects rvf_master database is available during startup.

### Create rvf_master database
```sql
create database rvf_master;
```
### Create a rvf_user if you don't want to use 'root'
```sql
create user 'rvf_user'@'localhost';
-- Alternatively give a password: create user 'rvf_user'@'localhost' IDENTIFIED BY 'password_here';
grant all privileges on *.* to 'rvf_user'@'localhost';
```

## Override the default config

### Default datasource configurations:
```properties
spring.datasource.username=root
spring.datasource.password=
spring.datasource.url=jdbc:mysql://localhost:3306/?useSSL=false
```
### Use a properties file to override default values
Create a properties file named **application-local.properties** with content:
```properties
spring.datasource.username=your_user_name
spring.datasource.password=your_password
```
Then start RVF with an extra JVM parameter:
```bash
--spring.config.additional-location=application-local.properties
```
