Release Validation Framework
============================
A framework for testing the validity of SNOMED CT files.

Build Instructions
------------------
```
git clone https://github.com/IHTSDO/release-validation-framework.git
mvn clean install
```

Testing Instructions
--------------------
To run unit tests use: 
```
mvn clean test
```

### Integration Testing
Integration tests expect an actual MySQL SNOMED CT database that contains SNOMED CT data. To run integration tests use: 
```
mvn clean integration-test
```

