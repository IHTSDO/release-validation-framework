# Getting started
## Prerequisites
- Java 17
- Maven 3

## Setup
### Install Mysql 8.0
RVF requires a local MySQL database to be available. 
#### Download and install [MySQL 8.0](https://dev.mysql.com/doc/refman/8.0/en/installing.html)
#### Or use brew on macOS
```bash
brew install mysql@8.0
```

Following [this](configuration-guide.md) to set up the database and user.

### Build RVF

```bash
mvn clean install
```

### Import assertions 
[Follow instructions here](importing-assertions.md).

### Start RVF application

```bash
java -Xms1024m -Xmx8g -Daws.region=us-east-1 -jar target/release-validation-framework*.jar --server.port=8081 --server.servlet.context-path=/api
```