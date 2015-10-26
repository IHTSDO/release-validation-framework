
/******************************************************************************** 
	file-centric-snapshot-stated-relationshipunique-id

	Assertion:
	The current Stated Relationship snapshot file does not contain duplicate 
	Relationship Ids

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		sourceid,
		concat('RELATIONSHIP: id=',id, ' is repeated in the Stated Relationship snapshot file.') 
	from curr_stated_relationship_s
	group by id
	having count(id) > 1;