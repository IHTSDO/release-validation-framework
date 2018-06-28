#/bin/bash
# Used identify any differences between relationship files from two different substrates.
# Two arguments must be given when running this script, they must be the paths
# to each of the relationship files to be compared.
# The following fields are compared: active, source, destination, type charType and modifier
# Content in the LOINC module will be excluded from the conparison.

a=$1
b=$2
echo "Comparing $a and $b"

# id	effectiveTime	active	moduleId	sourceId	destinationId	relationshipGroup	typeId	characteristicTypeId	modifierId
# 0   1             2       3         4         5             6                 7       8                     9
echo "Extracting relevant columns from active rows in $a and sorting"
# Filter out LOINC module 715515008
# Extract columns: active, source, destination, type charType and modifier
# Grep for rows starting with active=1
# Sort file ready for comparison
grep -v "\t715515008\t" $a | cut -f3,5,6,8,9 | grep "^1\t" | sort > a.txt

echo "Extracting relevant columns from active rows in $b and sorting"
grep -v "\t715515008\t" $b | cut -f3,5,6,8,9 | grep "^1\t" | sort > b.txt

echo
echo "Processing files ..."
comm -23 a.txt b.txt > a-unique.txt
comm -23 b.txt a.txt > b-unique.txt
comm -12 a.txt b.txt > a-b-common.txt
echo


echo "File a contains `wc -l a.txt | sed 's/ */ /' | cut -d ' ' -f2` active relationships = $a"
echo "File b contains `wc -l b.txt | sed 's/ */ /' | cut -d ' ' -f2` active relationships = $b"

echo
echo "File a contains `wc -l a-unique.txt | sed 's/ */ /' | cut -d ' ' -f2` rows which are not in b."
echo "See a-unique.txt"
echo
echo "File b contains `wc -l b-unique.txt | sed 's/ */ /' | cut -d ' ' -f2` rows which are not in a."
echo "See b-unique.txt"

echo
echo "There are `wc -l a-b-common.txt | sed 's/ */ /' | cut -d ' ' -f2` rows in common"
echo
echo "See a-unique.txt and b-unique.txt for the rows which are not in the other file."
echo
echo "Comparison complete."
