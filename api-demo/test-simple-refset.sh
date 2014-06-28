#!/bin/bash
#
# Command line statements which use the RVF API to test a simple refset
#
# Stop on error
set -e;
#
# Declare parameters
packageToTest="SnomedCT_test1_INT_20140131.zip"

# note port 8083
api="http://localhost:8080/api/v1"

#
echo
echo "Target Release Verification Framework API URL is '${api}'"
echo
#
echo "Upload Simple Refset"
# curl -iS -F "file=@${packageToTest}" ${api}/package-upload
curl -F file=@${packageToTest} ${api}/package-upload

echo
