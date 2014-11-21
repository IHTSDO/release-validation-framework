
/******************************************************************************** 
	release-type-snapshot-delta-validation-language-refset
	Assertion: The current data in the Language refset snapshot file are the 
	same as the data in the current delta file.  

********************************************************************************/
	/* selecting the latest components (i.e. the delta) from the snapshot */
	create or replace view vw as
	select * 
	from curr_langrefset_s
	where cast(effectivetime as datetime)= 
		(select max(cast(effectivetime as datetime))
		 from curr_langrefset_s);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('LANGUAGE REFSET: id=',a.id, ': Member in snapshot file, but not in delta file.') 	
	from vw a
	left join curr_langrefset_d b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.acceptabilityid = b.acceptabilityid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.referencedcomponentid is null
	or b.acceptabilityid is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('LANGUAGE REFSET: id=',a.id, ': Member in delta but not in snapshot file.') 	
	from curr_langrefset_d a
	left join vw b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.acceptabilityid = b.acceptabilityid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.referencedcomponentid is null
	or b.acceptabilityid is null;

	drop view vw;
	 
	 
	 
	 