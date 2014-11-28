Release Validation Framework
============================
A framework for testing the validity of SNOMED CT files.

Build Instructions
------------------
```
git clone https://github.com/IHTSDO/release-validation-framework.git
mvn clean install
```

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
mvn clean integration-test -Dskip.integration.tests=false
```

Note that all tests in the API that deal with controllers are currently marked as Integration tests. The spring context
used by the api-module tries to connect to a MySQL server, which will be missing in Jenkins. So to prevent needless test
failure on Jenkins, all these tests have been marked as IntegrationTests. This should be skipped by setting a separate 
Spring context file for tests that do not require MySQL access.

