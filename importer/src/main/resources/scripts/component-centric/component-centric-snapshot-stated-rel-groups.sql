
/******************************************************************************** 
	component-centric-snapshot-stated-rel-groups

	Assertion:
	Relationship groups contain at least 2 stated relationships.
.

********************************************************************************/
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('RELATIONSHIP: id=',a.id, ': Relationship is in a relationship group with a single active inferred member.') 	
	from curr_stated_relationship_s a
	where a.relationshipgroup != 0 
	and a.active ='1'
	group by a.sourceid, a.relationshipgroup
	having count(*) = 1;
