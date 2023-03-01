# Getting started
## Prerequisites
- Java 11
- Maven 3

## Setup
### Install Mysql 5.7
RVF requires a local MySQL database to be available. 
#### Download and install [MySQL 5.7](https://dev.mysql.com/doc/refman/5.7/en/installing.html)
#### Or use brew on macOS
```bash
brew install mysql@5.7
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
java -Xms512m -Xmx4g -jar /target/release-validation-framework*.jar --server.port=8081 --server.servlet.context-path=/api
```