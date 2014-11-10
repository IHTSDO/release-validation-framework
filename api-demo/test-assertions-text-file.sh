#!/bin/bash
#
# Command line statements which use the RVF API to test a simple refset
#
# Stop on error
set -e;
#
# Declare parameters
fileToTest="rel2_Refset_SimpleDelta_INT_20140131.txt"

# Target API Deployment
api="http://localhost:8080/api/v1"
#api="https://uat-rvf.ihtsdotools.org/api/v1"

#
echo
echo "Target Release Validation Framework API URL is '${api}'"
echo
#
echo "Creating a new Assertion"
#curl -F file=@${fileToTest} "${api}/test-file" | tr -d '"'| tee report.csv
curl -i \
  --header "Content-type: application/json" \
  --header "Accept: application/json" \
  -X POST localhost:8080/api/v1/assertions \
  --data '{"name":"Assertion 1","description": "My first assertion!"}'
echo

echo "Return list of all assertions"
curl --header "Accept: application/json" localhost:8080/api/v1/assertions/
echo

echo "Return assertion with specified id"
curl --header "Accept: application/json" localhost:8080/api/v1/assertions/1
echo

echo "Return assertion with specified id as XML"
curl --header "Accept: application/xml" localhost:8080/api/v1/assertions/1
echo

echo "Returning assertion with updated name"
curl -i \
  --header "Content-type: application/json" \
  --header "Accept: application/json" \
  -X PUT localhost:8080/api/v1/assertions/1 \
  --data '{"name":"Assertion 1 updated","description": "After updating name!"}'
echo

echo "Creating sample tests"
curl -i \
  --header "Content-type: application/json" \
  --header "Accept: application/json" \
  -X POST localhost:8080/api/v1/tests \
  --data '{"name":"Test 1"}'
curl -i \
  --header "Content-type: application/json" \
  --header "Accept: application/json" \
  -X POST localhost:8080/api/v1/tests \
  --data '{"name":"Test 2"}'
echo

echo "Linking tests with assertion"
curl -i \
  --header "Content-type: application/json" \
  --header "Accept: application/json" \
  -X POST localhost:8080/api/v1/assertions/1/tests \
  --data '[{"id":"1", "name":"Test 1"}, {"id":"2", "name":"Test 2"}]'
echo

echo "Getting tests associated with assertion"
curl -i \
  --header "Content-type: application/json" \
  --header "Accept: application/json" \
  -X GET localhost:8080/api/v1/assertions/1/tests
echo

echo "Deleting assertion with specified id"
curl -i \
  --header "Content-type: application/json" \
  --header "Accept: application/json" \
  -X DELETE localhost:8080/api/v1/assertions/1
#curl -i -X DELETE localhost:8080/api/v1/assertions/delete/1
echo

#echo "Deleting assertion with missing id"
#curl -i \
#  --header "Content-type: application/json" \
#  --header "Accept: application/json" \
#  -X DELETE localhost:8080/api/v1/assertions/delete/23863232
#echo

echo "Return list of all assertions - should be missing assertion with id 1"
curl --header "Accept: application/json" localhost:8080/api/v1/assertions/
echo