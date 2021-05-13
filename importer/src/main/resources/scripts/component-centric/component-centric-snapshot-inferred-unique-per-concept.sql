
/******************************************************************************** 
	component-centric-snapshot-inferred-unique-per-concept
	
	Assertion:
	No Concept has 2 inferred relationships with the same type, destination and group.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('Source id=', a.sourceid,' has two inferred relationships with same typeid and same destinationid within a single relationship-group.'),
		a.id,
		'curr_relationship_s'
	from curr_relationship_s a
	where a.active = '1'
	group by a.sourceid, a.typeid, a.destinationid, a.relationshipgroup
	having count(*) > 1;

	
	