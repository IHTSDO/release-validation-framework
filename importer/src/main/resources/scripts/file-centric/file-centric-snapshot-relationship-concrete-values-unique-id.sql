
/******************************************************************************** 
	file-centric-snapshot-relationship-concrete-values-unique-id

	Assertion:
	The current Relationship Concrete Values snapshot file does not contain duplicate
	Relationship Ids

********************************************************************************/
	
	
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		sourceid,
		concat('RELATIONSHIP CONCRETE VALUES: id=',id, ' is repeated in the Relationship Concrete Values snapshot file.'),
		id,
		'curr_relationship_concrete_values_s'
	from curr_relationship_concrete_values_s
	group by id
	having count(id) > 1;