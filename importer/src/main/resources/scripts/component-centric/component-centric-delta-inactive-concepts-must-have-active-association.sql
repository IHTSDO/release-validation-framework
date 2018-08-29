/*
 * MAINT-119
 * Concepts inactivated in this release cycle must have an active historical association
 * Unless the inactivation reason is "Non-conformance to editorial policy" in which 
 * case no association should be present.
 */
insert into qa_result (runid, assertionuuid, concept_id, details)
SELECT <RUNID>, '<ASSERTIONUUID>', c.id,
concat('Inactive concept (', c.id, '|', IFNULL(d.term, 'NO ACTIVE FSN FOUND'),'|) missing active historical association to active concept.')  
FROM curr_concept_d c
LEFT JOIN curr_description_s d 
	ON d.conceptid = c.id 
	AND d.active = 1 
	AND d.typeid = 900000000000003001 -- FSN 
LEFT JOIN curr_attributevaluerefset_s i 
	ON i.referencedcomponentid = c.id 
	AND i.active = 1 
	AND i.refsetid='900000000000489007' 
	AND NOT i.valueid = '723277005' -- Nonconformance to editorial policy component (foundation metadata concept) 
WHERE c.active = 0
AND NOT EXISTS (
  SELECT 1 FROM curr_associationrefset_s a, curr_concept_s c2
  WHERE a.referencedcomponentid = c.id
  AND a.active = 1
  AND a.targetcomponentid = c2.id
  AND c2.active = 1 );
 commit;
