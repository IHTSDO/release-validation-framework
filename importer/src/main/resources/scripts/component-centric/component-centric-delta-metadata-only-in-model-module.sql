/*
 * Metadata concepts must only exist in model component module.
 */
insert into qa_result (runid, assertionuuid, concept_id, details)
SELECT <RUNID>, '<ASSERTIONUUID>', c.id,  
concat('Metadata concept (', c.id, '|', d.term,'|) in module other than model module') 
FROM curr_description_s d, curr_concept_d c
WHERE d.conceptid = c.id
AND d.typeid = 900000000000003001 -- FSN
AND d.term LIKE '%(%metadata%)'
AND c.active = 1 
AND c.moduleId <> 900000000000012004 -- Model Module
AND d.active = 1;
 commit;
