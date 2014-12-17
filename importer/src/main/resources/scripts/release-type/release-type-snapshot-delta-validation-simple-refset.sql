
/******************************************************************************** 
	release-type-snapshot-delta-validation-simple-refset 

	Assertion: The current data in the Simple refset snapshot file are the same 
	as the data in the current delta file.

********************************************************************************/
	/* selecting the latest components (i.e. the delta) from the snapshot */

  drop table if exists vw;
  create table if not exists vw like curr_simplerefset_s;
  insert into vw
	select *
	from curr_simplerefset_s
	where effectivetime = '<CURRENT-RELEASE-DATE>';

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		a.id
	from vw a
	left join curr_simplerefset_d b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.referencedcomponentid = b.referencedcomponentid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.referencedcomponentid is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		a.id
	from curr_simplerefset_d a
	left join vw b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.referencedcomponentid = b.referencedcomponentid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.referencedcomponentid is null;

	drop table if exists vw;
	 
	 
	 
	 