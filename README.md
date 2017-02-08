Release Validation Framework ("RVF")
====================================
A framework for testing the validity of SNOMED CT releases.

Build Instructions
------------------
```
git clone https://github.com/IHTSDO/release-validation-framework.git
// -- see note below about config folder
mvn clean install
```

Database Setup
------------------
The RVF currently requires a local database to be available as per the settings defined in execution-service.properties below.
Setting up this database and user can be done using the following mysql code:
```
CREATE USER 'rvf_user'@'localhost' 
// alternatively more secure: CREATE USER 'rvf_user'@'localhost' IDENTIFIED BY 'password_here';

GRANT ALL PRIVILEGES ON *.* TO 'rvf_user'@'localhost';
```
The rvf_user should not be restricted to the rvf_master database schema, as it will be required to generate new databases for each release (both existing and prospective) that it receives.
### Configuration Folder
The services in the RVF can be configured using property files. Default values for the services are included in the jar 
files. However, it is possible to override the default values by providing property files for each of the services. 
The following is a list of property files that can be used to configure services:

|File name | Description | RVF deployment location |
|:------------- |:-------------:|:-----|
data-service.properties | Settings to configure the data store for RVF domain entities (assertions, test, etc) | /etc/opt/rvf-api/ |
execution-service.properties | Settings to configure the location of SNOMED CT data used by RVF | /etc/opt/rvf-api/ |
validation-service.properties | Settings to configure structural tests report location and threshold |  /etc/opt/rvf-api/ |

Sample files for configuring the services can be see found in the config folder.

Run standalone application
------------------
Start the application using the standalone executable jar which includes an embedded tomcat:

`java -jar api/target/validation-api.jar`

### Upload a Published Release
Option 1:
The release endpoint of the REST API can be used to list releases and to upload a published release.
Find the endpoint at **http://localhost:8080/api/v1/releases**

Example upload
```bash
curl -X POST -F 'file=@SnomedCT_RF2Release_INT_20160731.zip' http://localhost:8080/api/v1/releases/int/20160731
```
Option 2: Using Swagger api as shown below. See Manage published releases section for detail information.

Swagger API URL
--------------------
Find more information about API via Swagger. http://localhost:8080/api/v1/api-doc.html

Testing Instructions
--------------------
To run unit tests use: 
```
mvn clean test
```

### Integration Testing
Integration tests expect an actual MySQL SNOMED CT database that contains SNOMED CT data. To run integration tests use: 
```
mvn clean integration-test -Dskip.integration.tests=false -DrvfConfigLocation={config_dir}
```

Note that all tests in the API that deal with controllers are currently marked as Integration tests. The spring context
used by the api-module tries to connect to a MySQL server, which will be missing in Jenkins. So to prevent needless test
failure on Jenkins, all these tests have been marked as IntegrationTests. This should be skipped by setting a separate 
Spring context file for tests that do not require MySQL access.

Importing Assertions
--------------------
Assertions are currently imported from copies of the legacy implementation (RAT) configuration files included in this project.
```

