/*
 * MAINT-342
 * Concepts inactivated in this release cycle must have an active FSN
 * which is preferred in both US and GB Language Reference Sets
 */
insert into qa_result (runid, assertionuuid, concept_id, details)
SELECT <RUNID>, '<ASSERTIONUUID>', c.id,  
concat('Inactive concept (', c.id, '|', IFNULL(d1.term, 'NO ACTIVE FSN FOUND'),'|) missing active FSN preferred in en-gb AND en-us dialects.') 
FROM curr_concept_d c LEFT JOIN curr_description_s d1
	ON d1.conceptid = c.id 
	AND d1.typeid = 900000000000003001 -- FSN
	AND d1.active = 1
WHERE c.active = 0 
AND ( NOT EXISTS (
		SELECT 1 FROM curr_description_s d2, curr_langrefset_s l
		WHERE c.id = d2.conceptid 
		AND l.referencedcomponentid = d2.id
		AND l.active = 1
		AND d2.active = 1
		AND d2.typeid = 900000000000003001 -- |Fully specified name (core metadata concept)|
		AND l.acceptabilityid = 900000000000548007 -- |Preferred (foundation metadata concept)|
		AND l.refsetid =  900000000000508004 -- |Great Britain English language reference set (foundation metadata concept)|
		)
	OR NOT EXISTS (
		SELECT 1 FROM curr_description_s d2, curr_langrefset_s l
		WHERE c.id = d2.conceptid 
		AND l.referencedcomponentid = d2.id
		AND l.active = 1
		AND d2.active = 1
		AND d2.typeid = 900000000000003001 -- |Fully specified name (core metadata concept)|
		AND l.acceptabilityid = 900000000000548007 -- |Preferred (foundation metadata concept)|
		AND l.refsetid = 900000000000509007 -- |United States of America English language reference set (foundation metadata concept)|
		)
);
 commit;
