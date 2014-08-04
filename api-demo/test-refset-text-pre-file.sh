#!/bin/bash
#
# Command line statements which use the RVF API to test a simple refset
#
# Stop on error
set -e;
#
# Declare parameters
fileToTest="rel2_Description_Delta-en_INT_20240731.txt"

# Target API Deployment
api="http://localhost:8080/api/v1"
#api="https://uat-rvf.ihtsdotools.org/api/v1"

#
echo
echo "Target Release Verification Framework API URL is '${api}'"
echo
#
echo "Upload Simple Refset and Write out report.csv"
curl -i -X POST "$api/test-file" -F file=@${fileToTest} | tr -d '"'| tee report.csv
#curl -F file=@${packageToTest} ${api}/test-file | tr -d '"'| tee report.csv
