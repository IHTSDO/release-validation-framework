
/******************************************************************************** 
	component-centric-snapshot-stated-isa-relgroup
	
	Assertion:
	All stated is-a relationships have relationship group of 0.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('RELATIONSHIP: id=',a.id, ': Stated is-a relationship exists in a non-zero relationship group.') 	
	from curr_stated_relationship_s a
	where a.active = '1'
	and a.relationshipgroup > 0
	and typeid = '116680003';	
	