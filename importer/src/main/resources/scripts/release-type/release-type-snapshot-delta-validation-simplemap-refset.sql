/******************************************************************************** 
	release-type-snapshot-delta-validation-simplemap-refset 

	Assertion: The current data in the SimpleMap refset snapshot file are the 
	same as the data in the current delta file. 

********************************************************************************/
	/* selecting the latest components (i.e. the delta) from the snapshot */

  drop table if exists vw;
  create table if not exists vw like curr_simplemaprefset_s;
  insert into vw
	select *
	from curr_simplemaprefset_s
	where cast(effectivetime as datetime) >= 
		(select min(cast(effectivetime as datetime))
		 from curr_simplemaprefset_d);

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('SIMPLE REFSET: id=',a.id, ': Member in snapshot file, but not in delta file.') 	
	from vw a
	left join curr_simplemaprefset_d b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.maptarget = b.maptarget
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.referencedcomponentid is null
	or b.maptarget is null;

	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('SIMPLE REFSET: id=',a.id, ': Member in delta but not in snapshot file.') 	
	from curr_simplemaprefset_d a
	left join vw b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.referencedcomponentid = b.referencedcomponentid
		and a.maptarget = b.maptarget		
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.referencedcomponentid is null
	or b.maptarget is null;

	drop table if exists vw;
