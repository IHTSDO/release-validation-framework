Manual Installation
===================

These instructions will provide guidance in getting the RVF running on your local machine for development and testing using a manual configuration.


Prerequisites
-------------

You will need:
- [Git Client](https://git-scm.com/) to clone the project
- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads)
- [Maven](https://maven.apache.org/) to build


Build Instructions
------------------

Clone the project then use maven to build it:
```
mvn clean install
```

Database Setup
--------------

The RVF currently requires a local MySQL database to be available.
Setting up this database and user can be done using the following:

```sql
CREATE DATABASE rvf_master;

CREATE USER 'rvf_user'@'localhost';
// Alternatively give a password: CREATE USER 'rvf_user'@'localhost' IDENTIFIED BY 'password_here';

GRANT ALL PRIVILEGES ON *.* TO 'rvf_user'@'localhost';
```

Be sure to include details of the connection in the execution-service.properties file mentioned below.
The privileges of the user 'rvf_user' should not be restricted to the 'rvf_master' database because additional databases will be generated for each SNOMED release.

Configuration
-------------

There are various services that can be configured. There are default values but these can be overridden using properties files.
The following is a list of property files that can be used to configure services:

|File name | Description | RVF deployment location |
|:------------- |:-------------:|:-----|
data-service.properties | Settings to configure the data store for RVF domain entities (assertions, test, etc) | /etc/opt/rvf-api/ |
execution-service.properties | Settings to configure the location of SNOMED CT data used by RVF | /etc/opt/rvf-api/ |
validation-service.properties | Settings to configure structural tests report location and threshold |  /etc/opt/rvf-api/ |

Sample files for configuring the services can be found in the config folder.

Starting The Application
------------------------

Start the stand-alone application using the executable jar, replacing "{config_dir}" with an absolute path.

`java -Xms512m -Xmx4g -DrvfConfigLocation={config_dir} -jar api/target/api.jar --server.port=8081 --server.servlet.context-path=/api`

Testing Instructions

--------------------
To run unit tests use: 
```
mvn clean test
```

Integration Testing
-------------------

Integration tests require a MySQL database containing SNOMED CT data. To run integration tests once this is in place, use: 
```
mvn clean integration-test -Dskip.integration.tests=false -DrvfConfigLocation={config_dir}

```