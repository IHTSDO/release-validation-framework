
/******************************************************************************** 
	file-centric-snapshot-stated-relationshipunique-id

	Assertion:
	The current Stated Relationship snapshot file does not contain duplicate 
	Relationship Ids

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		sourceid,
		concat('RELATIONSHIP: id=',id, ' is repeated in the Stated Relationship snapshot file.'),
		id,
		'curr_stated_relationship_s'
	from curr_stated_relationship_s
	group by id
	having count(id) > 1;