#!/bin/bash
#
# Command line statements which use the RVF API to test a simple refset
#
# Stop on error
set -e;
#set -x;  #echo every statement executed
#
# Declare parameters
fileToTest="rel2_Refset_SimpleDelta_INT_20140131.txt"

# Target API Deployment
api="http://localhost:8080/api/v1"
#api="http://localhost:8081/api/v1"
#api="https://dev-rvf.ihtsdotools.org/api/v1"
#api="https://uat-rvf.ihtsdotools.org/api/v1"

#TODO make this function miss out the data if jsonFile is not specified.
function callURL() {
	httpMethod=$1
	url=$2
	jsonFile=$3
	dataArg=""
	if [ -n "${jsonFile}" ] 
	then
		jsonData=`cat ${jsonFile}`   #Parsing the json to objects in spring seems very forgiving of white space and unescaped characters.
		dataArg="${jsonData}"
	fi
	curl -i \
	--header "Content-type: application/json" \
	--header "Accept: application/json" \
	-X ${httpMethod} \
	-d "${dataArg}" \
	${url}
}

function askContinue() {
	read -p "Continue? (y/n): " user_choice
	case "$user_choice" in
	  n|N) echo "Calling a halt to the proceedings"; exit 0;;
	  *) echo 'OK, pressing on...';;
	esac
}

echo
echo "Target Release Validation Framework API URL is '${api}'"
echo

echo "Creating a new Assertion"
callURL POST ${api}/assertions json_files/create_assertion.json
echo

#TODO Recover the ID of the assertion created to use in subsequent tests
currentAssertion=1

askContinue

echo "Return list of all assertions"
callURL GET ${api}/assertions/
echo

echo "Return assertion with specified id"
callURL GET ${api}/assertions/${currentAssertion}
echo

echo "Return assertion with specified id as XML"
curl --header "Accept: application/xml" ${api}/assertions/1
echo

echo "Returning assertion with updated name"
callURL PUT ${api}/assertions/${currentAssertion} json_files/update_assertion_name.json
echo

askContinue

echo "Creating sample tests"
callURL POST ${api}/tests json_files/create_test_1.json
callURL POST ${api}/tests json_files/create_test_2.json
echo

echo "Linking tests with assertion"
callURL POST ${api}/assertions/${currentAssertion}/tests json_files/link_tests_to_assertion.json
echo

echo "Getting tests associated with assertion"
callURL GET ${api}/assertions/${currentAssertion}/tests
echo

echo "Deleting assertion with specified id"
callURL DELETE ${api}/assertions/1
echo

echo "Get assertion with missing id - should return 404 status"
callURL GET ${api}/assertions/233490734633
echo


echo "Return list of all assertions - should be missing assertion with id 1"
callURL GET ${api}/assertions/
echo
