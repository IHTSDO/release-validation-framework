
/******************************************************************************** 
	component-centric-snapshot-concept-primitive

	Assertion:
	The definition status is PRIMITIVE for concepts having only one defining relationship.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, concept_id, details, component_id, table_name)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ':Concept has only one defining relationship but the definition status is not primitive.'),
		a.id,
        'curr_concept_s'
	from curr_concept_s a 
	inner join curr_stated_relationship_s b	on a.id = b.sourceid
	where a.active = '1'
	and b.active = '1'
	and a.definitionstatusid != '900000000000074008'
	group by b.sourceid
	having count(*) = 1;
