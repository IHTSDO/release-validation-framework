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
api="http://localhost:8080/api/v1"
mkdir -p tmp

#TODO make this function miss out the data if jsonFile is not specified.
function callURL() {
	jsonFile=$3
	dataArg=""
	if [ -n "${jsonFile}" ] 
	then
		jsonData=`cat ${jsonFile}`   #Parsing the json to objects in spring seems very forgiving of white space and unescaped characters.
		dataArg="${jsonData}"
	fi
	callURL_JSON $1 $2 ${dataArg}
}

function callURL_JSON() {
	httpMethod=$1
	url=$2
	dataArg=$3
	curl -i --retry 0 \
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

function listAssertionsByGroupId() {
	echo
	read -p "Which group id to list?:" groupId
	echo "Listing Assertions for group id ${groupId}"
	callURL GET ${api}//groups/${groupId}/assertions/
} 

function listGroups() {
	echo
	echo "Listing Groups"
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
	read -p "What is the product name (eg int) ?: " product
	read -p "What is the release date?: " version
	
	read -p "Do you wish to append an extension to an international release? (Y/N): " append
	appendStr="false"
	if [ ${append} == "y" ] ||  [ ${append} == "Y" ] 
	then
		appendStr="true"
	fi
	
	url=" ${api}/releases/${product}/${version}"
	echo "Uploading release file to ${url} with append = "
	curl --retry 0 -X POST ${url} --progress-bar -F file=@${releaseFile} \
		 -F "append=${appendStr}" \
		 -o tmp/uploadprogress.txt
}

function doTest() {
	testType=$1
	echo
	if [ ${testType} != "single" ]
	then
		read -p "What archive should be uploaded (prospective release)?: " releaseFile
		if [ ! -e ${releaseFile} ]
		then
			echo "${releaseFile} not found. "
			return
		elif [ -d $releaseFile ] 
		then
			echo "Can't pass a whole directory!"
			return
		elif [ -z $releaseFile ] 
		then
			echo "Passing empty file parameter - set purge to false!"
			fileParam=""
		else
			fileParam="-F file=@${releaseFile}" 
		fi

        read -p "Is prospective file only containing RF2 Delta(Y/N) ?: " isDeltaExport
            deltaOnlyParamStr="false"
        if [ ${isDeltaExport} == "y" ] ||  [ ${isDeltaExport} == "Y" ]
        then
            deltaOnlyParamStr="true"
        fi

		read -p "What manifest should be uploaded?: " manifestFile
		if [ ! -e ${manifestFile} ] 
		then
			echo "${manifestFile} not found."
			return
		elif [ -z $manifestFile ] 
		then
			echo "Passing empty manifestFile file parameter!"
			manifestFileParam=""
		else
			manifestFileParam="-F file=@${manifestFile}" 
		fi
	fi

	datestamp=`date +%Y%m%d%H%M%S`

	if [ ${testType} == "structural" ] 
	then
		curl -i -X POST "$api/test-post" -F manifest=@${manifestFile} ${fileParam}
	elif [ ${testType} == "single" ]
	then 
		read -p "What assertion id should be used?: " assertionId
		read -p "What is the current (ie the one before the prospective one being tested) release version: " currentReleaseVersion
		read -p "What is the prospective (ie the one being tested) release version: " prospectiveReleaseVersion
		curl --retry 0 -i -X POST "${api}/assertions/${assertionId}/run" \
		--progress-bar \
		-F "prospectiveReleaseVersion=${prospectiveReleaseVersion}" \
		-F "previousReleaseVersion=${currentReleaseVersion}" \
		-F "runId=${datestamp}" 			 
	elif [ ${testType} == "full" ] ||	[ ${testType} == "extension" ]
	then
		read -p "What assertion group id(s) / name(s) should be used? (comma separate): " assertionGroups
		if [ ${testType} == "extension" ]
		then
			read -p "What is the dependency (ie the International Release that is being extended) release version: " extensionDependencyRelease
			read -p "What is the previous Extension release version: " previousExtensionVersion
		fi
		read -p "What is the previous International release version: " prevReleaseVersion
		curl --retry 0 -i -X POST "$api/run-post" \
		--progress-bar \
		${fileParam} \
		${manifestFileParam} \
		-F "previousIntReleaseVersion=${prevReleaseVersion}" \
		-F "extensionDependencyReleaseVersion=${extensionDependencyRelease}" \
		-F "previousExtensionReleaseVersion=${previousExtensionVersion}" \
		-F "groups=${assertionGroups}" \
		-F "runId=${datestamp}" \
		-F "storageLocation=RVFMisc/${datestamp}" \
        -F "rf2DeltaOnly=${deltaOnlyParamStr}" \
		-o tmp/uploadprogress.txt

		echo "Server call complete.  Server returned:  "
		cat tmp/uploadprogress.txt
		echo
	else
		echo "Test type ${testType} not recognised"
	fi
}

function groupAllAssertions() {

	_createGroup
	if [ -n "${newGroupId}" ]
	then
		echo "Grouping assertions under id ${newGroupId}"
		callURL PUT ${api}/groups/${newGroupId}/addAllAssertions
	else
		echo "Failed to create group"
		exit -1
	fi
	
}

function groupSpecifiedAssertions() {
	_createGroup	
	listAssertions | grep "\"id\"\|name" | sed "s/\"id\" ://" | sed "s/\"name\" ://" | sed "s/ \{8\}//" | paste - - | sort -k1,1n
	
	read -p "What assertion ids should be added to this group (comma separate): " assertionList
	
	IFS=","  #Set the internal field separator
	requestBody=""
	for assertion in ${assertionList} ; do
		if [ -n "${requestBody}" ] 
		then
			requestBody="${requestBody},"
		fi
		requestBody="${requestBody} \"${assertion}\""
	done
	requestBody="[${requestBody}]"
	callURL_JSON POST ${api}/groups/${newGroupId}/assertions "${requestBody}"
}

function _createGroup() {
	
	echo
	read -p "What group name should be used?: " groupName
	mkdir -p tmp
	#create the group and recover the ID
	echo "First creating an empty group using name ${groupName}"
	curl -X POST --data "name=${groupName}" ${api}/groups  | tee tmp/group-create-response.txt 
	newGroupId=`cat tmp/group-create-response.txt | grep "\"id\"" | sed 's/[^0-9]//g'`	
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
	echo "1 - test a package against a single assertion"
	echo "a - list known assertions"
	echo "b - list known groups"
	echo "c - list assertions by group ID"
	echo "e - test an extension specifying baseline and previous"
	echo "g - group all known assertions"
	echo "h - group specified assertions"
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
			1)   doTest "single"; break;;
			a|A) listAssertions ; break ;;
			b|B) listGroups ; break ;;
			c|C) listAssertionsByGroupId; break;;
			e|E) doTest "extension"; break;;
			l|L) listKnownReleases ; break;;
			g|G) groupAllAssertions; break;; 
			h|H) groupSpecifiedAssertions; break;; 
			s|S) doTest "structural"; break;;
			t|T) doTest "full"; break;;
			u|U) uploadRelease ; break;;
			q|Q) echo -e "\nQuitting..."; exit 0;;
		esac
	done
}

echo
echo "Target Release Validation Framework API URL is '${api}'"

while true
do
	mainMenu
	pressAnyKey
done

echo "Program exited unexpectedly"
