#!/bin/bash
#
# Command line statements which use the RVF API to test a simple refset
#

# Stop on error
set -e;

# Declare parameters
packageToTest="packages/SnomedCT_test1_INT_20140131.zip"
api="http://localhost:8080/rvf/api/v1"


echo
echo "Target Release Verification Framework API URL is '${api}'"
echo

echo "Upload Simple Refset"
curl -iS -F "file=@${packageToTest}" ${api}/test-package
echo
