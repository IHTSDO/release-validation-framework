Release Validation Framework - API Demo
=======================================
This module includes a set of curl based scripts to interact with the RVF API. The curl based methods show you how to
interact with the RVF API from non Java development languages.

Testing Instructions
--------------------
Assuming you've checked out RVF code, To run curl based tests use: 
```bash
# change to api module folder
cd api
# start tomcat 
mvn clean tomcat7:run
# open another terminal instance and go to api-demo module folder
cd api-demo
# run the test assertions group script
./test-assertions-group-file.sh
```

### Simple Testing
If you would like to see easy to understand examples of the CRUD methods in the API, follow the steps below: 
```bash
# change to api module folder
cd api
# start tomcat 
mvn clean tomcat7:run
# open another terminal instance and go to api-demo module folder
cd api-demo
# run the test assertions script -- note this file is different from the previous one
./test-assertions-text-file.sh
```

Note that the curl based tests complement the Spring MVC based tests that are included in the API folder

