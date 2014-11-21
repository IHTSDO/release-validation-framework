
/******************************************************************************** 
	component-centric-snapshot-inferred-unique-per-concept
	
	Assertion:
	No Concept has 2 inferred relationships with the same type, destination and group.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept have two inferred relationships with same typeid and same destinationid within a single relationship-group.') 	
	from curr_relationship_s a
	where a.active = '1'
	group by a.sourceid, a.typeid, a.destinationid, a.relationshipgroup
	having count(*) > 1;

	
	