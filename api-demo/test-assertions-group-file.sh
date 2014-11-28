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

echo "Create & Linking executable tests with assertion"
callURL POST ${api}/assertions/${currentAssertion}/tests json_files/executable_test.json
echo

echo "Getting tests associated with assertion"
callURL GET ${api}/assertions/${currentAssertion}/tests
echo

askContinue

echo "Creating a new Assertion Group"
curl -X POST --data "name=TestAssertionGroup" ${api}/groups
echo

currentGroup=1

echo "Create & Linking assertions with group"
callURL POST ${api}/groups/${currentGroup}/assertions json_files/link_assertions_to_group.json
echo

packageToTest="SnomedCT_test1_INT_20140131.zip"
echo "Upload Release Pack and run tests using group id"
#echo -e `curl -F file=@${packageToTest} ${api}/test-file` | tr -d '"'| tee report.csv
# load data from external file
groups= cat json_files/groups_to_execute.json
curl -X POST -F writeSuccesses="true" -F prospectiveReleaseVersion="20140731" -F previousReleaseVersion="20140731" -F runId="1" -F groups=${groups} -F file=@${packageToTest} ${api}/run-post