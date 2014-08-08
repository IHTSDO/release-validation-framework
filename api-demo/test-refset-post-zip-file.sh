#!/bin/bash
#
# Command line statements which use the RVF API to test a simple refset
#
# Stop on error
set -e;
#
# Declare parameters
packageToTest="ValidPostconditionAll.zip"

# Target API Deployment
api="http://localhost:8080/api/v1"
#api="http://localhost:8080/api/v1"
#api="https://uat-rvf.ihtsdotools.org/api/v1"

#
echo
echo "Target Release Verification Framework API URL is '${api}'"
echo
#
echo "Upload Simple Refset and Write out report.csv"
echo -e `curl -F file=@${packageToTest} ${api}/test-post` | tr -d '"'| tee report.csv

echo
