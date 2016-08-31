
/******************************************************************************** 
	release-type-snapshot-unique-relationship-id.sql

	Assertion:
	The ids should be unique across the stated and inferred relationship snapshot files.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Relationship: id=',a.id, ' is duplicated in the stated and inferred relationships') 	
	from curr_stated_relationship_s a, curr_relationship_s b
	where a.id=b.id;