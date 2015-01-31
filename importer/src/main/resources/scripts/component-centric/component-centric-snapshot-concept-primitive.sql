
/******************************************************************************** 
	component-centric-snapshot-concept-primitive

	Assertion:
	All Concepts having only one defining relationship have definition status 
	PRIMITIVE.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ':Concept has only one defining relationship but is not primitive.') 	
	from curr_concept_s a 
	inner join curr_stated_relationship_s b	on a.id = b.sourceid
	where a.active = '1'
	and b.active = '1'
	and a.definitionstatusid != '900000000000074008'
	group by b.sourceid
	having count(*) = 1;
