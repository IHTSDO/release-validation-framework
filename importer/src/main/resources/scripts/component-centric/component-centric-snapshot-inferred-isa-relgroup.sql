
/******************************************************************************** 
	component-centric-snapshot-inferred-isa-relgroup
	
	Assertion:
	All inferred is-a relationships have relationship group of 0.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.sourceid,
		concat('RELATIONSHIP: id=',a.id, ': Inferred is-a relationship exists in a non-zero relationship group.'),
		a.id,
        'curr_relationship_s'
	from curr_relationship_s a
	where a.typeid = '116680003'
	and a.relationshipgroup > 0;
