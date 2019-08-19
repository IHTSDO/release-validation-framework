/******************************************************************************** 
component-centric-snapshot-core-module-concepts-have-core-module-parents.sql
Assertion:
Core module concepts have core module parents.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		child_concept.id,
		concat('Core module concept ', child_concept.id, ' has Model module parent ')
    from curr_concept_s child_concept, curr_concept_s parent_concept,
    curr_description_s child_desc, curr_description_s parent_desc,
    curr_relationship_s r
    where child_concept.active = 1
    and parent_concept.active = 1
    and child_desc.active = 1
    and parent_desc.active = 1
    and child_desc.conceptid = child_concept.id
    and parent_desc.conceptid = parent_concept.id
    and child_desc.typeid = 900000000000003001 -- FSN
    and parent_desc.typeid = 900000000000003001 -- FSN
    and r.active = 1
    and r.sourceid = child_concept.id
    and r.destinationid = parent_concept.id
    and r.typeid = '116680003' -- IS A
    and parent_concept.moduleid = '900000000000012004' -- Model Module
    and child_concept.moduleid = '900000000000207008'; -- Core Module