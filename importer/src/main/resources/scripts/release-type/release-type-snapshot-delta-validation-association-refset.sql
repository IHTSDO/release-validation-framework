
/******************************************************************************** 
	release-type-snapshot-delta-validation-association-refset
	Assertion: The current data in the Association refset snapshot file are the same as the data in the current delta file. 

********************************************************************************/
	
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ASSOCIATION REFSET: id=',a.id, ' is in delta but not in snapshot file.') 	
	from curr_associationrefset_d a
	left join curr_associationrefset_s b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.targetcomponentid = b.targetcomponentid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.referencedcomponentid is null
	or b.targetcomponentid is null;
	