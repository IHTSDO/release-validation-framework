
/*  
	The current full association refset file consists of the previously published full file and the changes for the current release
*/
drop table if exists temp_table;

/* view of current delta, derived from current full */
	create table if not exists temp_table like prev_associationrefset_f;
	
	insert into temp_table 
	select * from curr_associationrefset_d;
	
	insert into temp_table 
	select *	from prev_associationrefset_f;

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Association refset: id=',a.id, ': Association refset is in current full file, but not in prior full file.') 	
	from curr_associationrefset_f a
	left join temp_table b 
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.targetcomponentid = b.targetcomponentid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
  	or b.refsetid is null
  	or b.referencedcomponentid is null
  	or b.targetcomponentid is null;

/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Association refset: id=',a.id, ': Association refset is in prior full file, but not in current full file.')
	from temp_table a
	left join curr_associationrefset_f b 
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.targetcomponentid = b.targetcomponentid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
  	or b.refsetid is null
  	or b.referencedcomponentid is null
  	or b.targetcomponentid is null;

commit;
drop table temp_table;
