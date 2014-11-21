
/******************************************************************************** 
	component-centric-snapshot-language-inactive-members

	Assertion:
	Members are inactive for inactive descriptions in the snapshot file.

********************************************************************************/
	
	/* 
	
	*/
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('MEMBER: id=',b.id, ': Refset member is active for an inactive description.') 
	from curr_description_d a
	inner join curr_langrefset_d b 
		on a.id = b.referencedcomponentid
	where a.active != b.active;

	