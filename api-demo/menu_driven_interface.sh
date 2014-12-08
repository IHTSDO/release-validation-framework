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
#TODO - allow the user to change the API at runtime
#api="http://localhost:8080/api/v1"
#api="http://localhost:8081/api/v1"
api="https://dev-rvf.ihtsdotools.org/api/v1"
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
	echo
}

function getReleaseDate() {
	releaseDate=`echo $1 | sed 's/[^0-9]//g'`
	if [ -z $releaseDate ] 
	then
		echo "Failed to find release date in $1.\nScript halting"
		exit -1
	fi
	echo $releaseDate
}

function listAssertions() {
	echo
	echo "Listing Assertions"
	callURL GET ${api}/assertions/
} 

function listGroups() {
	echo
	echo "Listing Assertions"
	callURL GET ${api}/groups/
} 

function listKnownReleases() {
	echo
	echo "Listing Known Releases:"	
	callURL GET ${api}/releases
}

function uploadRelease() {
	echo
	read -p "What file should be uploaded?: " releaseFile
	#Check file exists
	if [ ! -e ${releaseFile} ] 
	then
		echo "${releaseFile} not found."
		return
	fi
	releaseDate=`getReleaseDate ${releaseFile}`
	url=" ${api}/releases/${releaseDate}"
	echo "Uploading release file to ${url}"
	curl -X POST -F file=@${releaseFile} ${url} 
}

function doTest() {
	testType=$1
	echo
	read -p "What archive should be uploaded?: " releaseFile
	if [ ! -e ${releaseFile} ] 
	then
		echo "${releaseFile} not found."
		return
	fi
	prospectiveReleaseVersion=`getReleaseDate ${releaseFile}`
	read -p "What manifest should be uploaded?: " manifestFile
	if [ ! -e ${manifestFile} ] 
	then
		echo "${manifestFile} not found."
		return
	fi
	
	if [ ${testType} == "structural" ] 
	then
		curl -i -X POST "$api/test-post" -F manifest=@${manifestFile} -F file=@${releaseFile}
	elif  [ ${testType} == "full" ] 
	then
		read -p "What assertion group name should be used?: " assertionGroup
		read -p "What is the current (ie the one before the prospective one being tested) release version (YYYYMMDD): " currentReleaseVersion
		datestamp=`date +%Y%m%d%H%M%S`
		curl -i -X POST "$api/run-post" -F manifest=@${manifestFile} -F file=@${releaseFile} \
		-F "prospectiveReleaseVersion=${prospectiveReleaseVersion}" \
		-F "previousReleaseVersion=${currentReleaseVersion}" \
		-F "groups=${assertionGroup}" \
		-F "runId=${datestamp}" 
	else
		echo "Test type ${testType} not recognised"
	fi
}

function groupAllAssertions() {
	echo
	read -p "What group name should be used?: " groupName
	mkdir -p tmp
	#create the group and recover the ID
	echo "First creating an empty group using name ${groupName}"
	curl -X POST --data "name=${groupName}" ${api}/groups  | tee tmp/group-create-response.txt 
	newGroupId=`cat tmp/group-create-response.txt | grep "\"id\"" | sed 's/[^0-9]//g'`
	
	if [ -n "${newGroupId}" ]
	then
		echo "Grouping assertions under id ${newGroupId}"
		callURL PUT ${api}/groups/${newGroupId}/addAllAssertions
	else
		echo "Failed to create group"
		exit -1
	fi
	
}

function pressAnyKey() {

	echo "Hit any key to continue..."
	while :
	do
		read -s -n 1 user_choice
		case "$user_choice" in
			*) break;;
		esac
	done
}


function mainMenu() {
	echo 
	echo "*****   RVF Menu    ******"
	echo "a - list known assertions"
	echo "b - list known groups"
	echo "g - group all known assertions"
	echo "l - List known previous releases"
	echo "s - structural test a package with a manifest"
	echo "t - full test a package with a manifest"
	echo "u - Upload a previous release"
	echo "q - quit"
	echo
	echo -n "Please select:"
	while :
	do
		read -s -n 1 user_choice
		case "$user_choice" in
			a|A) listAssertions ; break ;;
			b|B) listGroups ; break ;;
			l|L) listKnownReleases ; break;;
			g|G) groupAllAssertions; break;; 
			s|S) doTest "structural"; break;;
			t|T) doTest "full"; break;;
			u|U) uploadRelease ; break;;
			q|Q) echo -e "\nQuitting..."; exit 0;;
		esac
	done
}

echo
echo "Target Release Validation Framework API URL is '${api}'"
echo

while true
do
	mainMenu
	pressAnyKey
done

echo "Program exited unexpectedly"
