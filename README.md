Release Validation Framework (RVF)  [![Join the chat at https://gitter.im/IHTSDO/release-validation-framework](https://badges.gitter.im/IHTSDO/release-validation-framework.svg)](https://gitter.im/IHTSDO/release-validation-framework?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
====================================

A framework for testing the validity of SNOMED CT releases.

Getting started
---------------
These instructions will provide guidance in getting the RVF running on your local machine for development and testing.

### Prerequisites
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
------------------
The RVF currently requires a local MySQL database to be available.
Setting up this database and user can be done using the following:
```
CREATE DATABASE rvf_master;

CREATE USER 'rvf_user'@'localhost';
// Alternatively give a password: CREATE USER 'rvf_user'@'localhost' IDENTIFIED BY 'password_here';

GRANT ALL PRIVILEGES ON *.* TO 'rvf_user'@'localhost';
```
Be sure to include details of the connection in the execution-service.properties file mentioned below.
The privileges of the user 'rvf_user' should not be restricted to the 'rvf_master' database because additional databases will be generated for each SNOMED release.

### Configuration
There are various services that can be configured. There are default values but these can be overridden using properties files.
The following is a list of property files that can be used to configure services:

|File name | Description | RVF deployment location |
|:------------- |:-------------:|:-----|
data-service.properties | Settings to configure the data store for RVF domain entities (assertions, test, etc) | /etc/opt/rvf-api/ |
execution-service.properties | Settings to configure the location of SNOMED CT data used by RVF | /etc/opt/rvf-api/ |
validation-service.properties | Settings to configure structural tests report location and threshold |  /etc/opt/rvf-api/ |

Sample files for configuring the services can be found in the config folder.

Starting The Application
------------------
Start the stand-alone application using the executable jar, replacing "{config_dir}" with an absolute path.

`java -Xms512m -Xmx4g -DrvfConfigLocation={config_dir} -jar api/target/api.jar --server.port=8081 --server.servlet.context-path=/api`

API Documentation
--------------------
The RVF API is documented using Swagger http://localhost:8081/api/swagger-ui.html

### Upload a Published Release
Option 1:
The release endpoint of the REST API can be used to list releases and to upload a published release.
Find the endpoint at **http://localhost:8081/api/releases**

Example upload
```bash
curl -X POST -F 'file=@SnomedCT_RF2Release_INT_20160731.zip' http://localhost:8081/api/releases/int/20160731
```
Option 2: Using Swagger API as shown above. See Manage published releases section for detailed information.

Testing Instructions
--------------------
To run unit tests use: 
```
mvn clean test
```

### Integration Testing
Integration tests require a MySQL database containing SNOMED CT data. To run integration tests once this is in place, use: 
```
mvn clean integration-test -Dskip.integration.tests=false -DrvfConfigLocation={config_dir}

```

Importing Assertions
--------------------
Assertions are imported automatically during RVF application startup. The list of assertions is documented in the manifest.xml file under importer/src/main/resources/xml/lists/ folder. Actual assertion SQL files can be found in the importer/src/main/resources/scripts folder.

