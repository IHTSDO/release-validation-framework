#!/bin/bash
# Directory to store the checked out resources
DROOLS_RULES_DIR=snomed-drools-rules
ASSERTIONS_DIR=snomed-release-validation-assertions
# Remove the directory if it exists
rm -rf $DROOLS_RULES_DIR
rm -rf $ASSERTIONS_DIR


# Clone the repository containing the resources
git clone https://github.com/IHTSDO/snomed-drools-rules.git $DROOLS_RULES_DIR

git clone https://github.com/IHTSDO/snomed-release-validation-assertions.git $ASSERTIONS_DIR
