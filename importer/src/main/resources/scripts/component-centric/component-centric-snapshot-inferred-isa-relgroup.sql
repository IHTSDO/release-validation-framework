
/******************************************************************************** 
	component-centric-snapshot-inferred-isa-relgroup
	
	Assertion:
	All inferred is-a relationships have relationship group of 0.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('RELATIONSHIP: id=',a.id, ': Inferred is-a relationship exists in a non-zero relationship group.') 	
	from curr_relationship_s a
	where a.relationshipgroup > 0
	and a.typeid = '116680003';	
	