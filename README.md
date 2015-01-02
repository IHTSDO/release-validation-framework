Release Validation Framework
============================
A framework for testing the validity of SNOMED CT releases.

Build Instructions
------------------
```
git clone https://github.com/IHTSDO/release-validation-framework.git
// -- see note below about config folder
mvn clean install -DrvfConfigLocation=/tmp
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

### Data Folder
The RVF provides a convenient feature that allows published releases to be uploaded and stored to a designated 'sct data' 
folder. During startup, the RVF unzips any published release in this folder and loads it into a database as a known release. 
The following conventions are used by this feature:

|File name | Schema name generated | Comment |
|:------------- |:-------------:|:-----|
SnomedCT\_Release\_INT\_20140731.zip | rvf\_int\_20140731 | Created if database named rvf\_int\_20140731 does not exist |
SnomedCT\_Release\_INT\_20140731.txt | Not processed | Only zip files are processed |

How to use the API
--------------------
While more comprehensive documentation is being prepared, you can find examples of how use the API in the test cases 
included in the API module. If you are not a Java developer and want to access the API from a non Java environment, 
then refer to the curl tests/examples in the API-Demo module.

Testing Instructions
--------------------
To run unit tests use: 
```
mvn clean test
```

### Integration Testing
Integration tests expect an actual MySQL SNOMED CT database that contains SNOMED CT data. To run integration tests use: 
```
mvn clean integration-test -Dskip.integration.tests=false - DrvfConfigLocation=/tmp
```

Note that all tests in the API that deal with controllers are currently marked as Integration tests. The spring context
used by the api-module tries to connect to a MySQL server, which will be missing in Jenkins. So to prevent needless test
failure on Jenkins, all these tests have been marked as IntegrationTests. This should be skipped by setting a separate 
Spring context file for tests that do not require MySQL access.

