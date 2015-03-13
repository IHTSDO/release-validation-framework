
/******************************************************************************** 
	release-type-snapshot-delta-validation-association-refset
	Assertion: The current data in the Association refset snapshot file are the same as the data in the current delta file. 

********************************************************************************/
	/* selecting the latest components (i.e. the delta) from the snapshot */

  drop table if exists vw;
  create table if not exists vw like curr_associationrefset_s;
  insert into vw
	select *
	from curr_associationrefset_s
	where cast(effectivetime as datetime) >= 
		(select min(cast(effectivetime as datetime))
		 from curr_associationrefset_d);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ASSOCIATION REFSET: id=',a.id, ': Concept in snapshot file, but not in delta file.') 	
	from vw a
	left join curr_associationrefset_d b
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

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ASSOCIATION REFSET: id=',a.id, ': Concept in delta but not in snapshot file.') 	
	from curr_associationrefset_d a
	left join vw b
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

	drop table if exists vw;
	 
	 
	 
	 