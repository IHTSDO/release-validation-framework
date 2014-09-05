#!/bin/bash
#
# Command line statements which use the RVF API to check a release archive with a specified manifest
#
# Stop on error
set -e;

#
# Recover Command Line Arguments
manifestFile=$1
packageToTest=$2

# Check command line arguments
if [ -z "$packageToTest" ]
then
	echo -e "Usage\n-------\nrun.sh <manifest location> <archive location>\nScript halted."
	# eg ./run.sh manifest_20150131.xml /Users/Peter/tmp/SRS_Daily_Build_20150131.zip
	exit -1	
fi


# Target API Deployment
#api="http://localhost:8080/api/v1"
#api="https://uat-rvf.ihtsdotools.org/api/v1"
api="https://rvf.ihtsdotools.org/api/v1"

echo
echo "Target Release Verification Framework API URL is '${api}'"
echo "Uploading manifest: $manifestFile and archive: $packageToTest"

curl -i -X POST "$api/test-post" -F manifest=@${manifestFile} -F file=@${packageToTest} | tr -d '"'

