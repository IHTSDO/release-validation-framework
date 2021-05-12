
/******************************************************************************** 
	file-centric-snapshot-inferred-relationship-unique-id

	Assertion:
	The current Inferred Relationship snapshot file does not contain duplicate 
	Relationship Ids

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		sourceid,
		concat('RELATIONSHIP : id=',id, ': Relationship Id is repeated in the Inferred Relationship snapshot file.'),
		id,
		'curr_relationship_s'
	from curr_relationship_s
	group by id
	having count(id) > 1;