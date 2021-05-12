
/******************************************************************************** 
	component-centric-snapshot-stated-unique-per-concept
	
	Assertion:
	No Concept has 2 stated relationships with the same type, destination and group.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('CONCEPT: id=',a.sourceid, ': Concept has two stated relationships with same typeid and destinationid within a single relationship group.'),
		a.id,
        'curr_stated_relationship_s'
	from curr_stated_relationship_s a
	where a.active = '1'
	group by a.sourceid, a.typeid, a.destinationid, a.relationshipgroup
	having count(*) > 1; 
