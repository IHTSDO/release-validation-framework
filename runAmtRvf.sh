#!/bin/sh

RESPONSE=$(curl --write-out '%{http_code}' --silent --output /dev/null http://localhost:8081/api/assertions/1)

if [ $RESPONSE -eq 404 ]
then
	echo "You have not yet created any test assertions. Will create them now."

  # -- CREATE AN ASSERTION --
  curl 'http://localhost:8081/api/assertions' \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    --data-raw $'{\n    "name": "I can create a custom AMT test",\n    "statement": null,\n    "description": "I can create a custom AMT test",\n    "docLink": null,\n    "effectiveFrom": null,\n    "keywords": null,\n    "groups": []\n}' \
    --compressed
  
  sleep 1
  
  # -- CREATE 2 TESTS --
  curl 'http://localhost:8081/api/assertions/1/tests' \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    --data-raw $'[\n   {\n       "id":null,\n       "name":"Custom AMT Test 1",\n       "description":null,\n       "type":"SQL",\n       "command":{"id":null,\n           "configuration":{"id":null,"items":[],"keys":[]},\n           "template":"select count(1) from <PROSPECTIVE>.concept_<SNAPSHOT> a where active = 1;",\n           "statements":[]}\n   }\n,\n   {\n       "id":null,\n       "name":"Custom AMT Test 2",\n       "description":null,\n       "type":"SQL",\n       "command":{"id":null,\n           "configuration":{"id":null,"items":[],"keys":[]},\n           "template":"select count(1) from <PROSPECTIVE>.concept_<SNAPSHOT> a where true = false;",\n           "statements":[]}\n   }\n]' \
    --compressed

  sleep 1
  
  # -- CREATE AN AMT ASSERTION GROUP --
  curl 'http://localhost:8081/api/groups?name=amt' \
    -X 'POST' \
    -H 'Accept: */*' \
    -H 'Content-Length: 0' \
    -H 'Content-Type: application/json' \
    --compressed
  
  sleep 1
  
  # -- ADD ASSERTION TO GROUP --
  curl 'http://localhost:8081/api/groups/41/assertions' \
    -H 'Accept: */*' \
    -H 'Content-Type: application/json' \
    --data-raw '["1"]' \
    --compressed || true

  sleep 1
  
fi

TEST_RUN_ID=`date +%y%m%d%H%M%S`
echo "Executing test run:\t$TEST_RUN_ID"

# -- START RVF EXECUTION --
curl --location --request POST "http://localhost:8081/api/run-post?rf2DeltaOnly=false&writeSuccesses=true&groups=amt&runId=$TEST_RUN_ID&failureExportMax=10&storageLocation=rctest&enableTraceabilityValidation=false&enableChangeNotAtTaskLevelValidation=false" \
  --header 'Accept: */*' \
  --form 'file=@"/opt/rvf/CSIRO_AUEdition_PRODUCTION_32506021000036107-20221031T100001Z.zip"'