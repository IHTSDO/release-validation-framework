
/******************************************************************************** 
	release-type-snapshot-unique-relationship-id.sql

	Assertion:
	The ids should be unique across the stated and inferred relationship snapshot files.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		duplicateRel.sourceid,
		concat('Stated Relationship: id=', duplicateRel.id, ' is duplicated in the inferred relationship snapshot') 	
	from (select distinct a.id,a.sourceid from curr_stated_relationship_s a, curr_relationship_s b, curr_stated_relationship_d c
	where a.id=c.id
	and a.id=b.id ) duplicateRel;
	
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		duplicateRel.sourceid,
		concat('Relationship: id=', duplicateRel.id, ' is duplicated in the stated relationship snapshot') 	
	from 
	(select distinct a.id,a.sourceid from curr_relationship_s a, curr_stated_relationship_s b, curr_relationship_d c
	where a.id=c.id
	and a.id=b.id ) duplicateRel;