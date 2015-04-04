
/*  
	The current full association refset file consists of the previously published full file and the changes for the current release
*/
drop table if exists v_temp_table;

/* view of current delta, derived from current full */
	create table if not exists v_temp_table like prev_associationrefset_f;
	
	insert into v_temp_table
	select * from curr_associationrefset_d;
	
	insert into v_temp_table
	select *	from prev_associationrefset_f;

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Association refset: id=',a.id, ' is in the current full file, but not in the prior full file.') 	
	from curr_associationrefset_f a
	left join v_temp_table b
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
	concat('Association refset: id=',a.id, ' is in the prior full file, but not in the current full file.')
	from v_temp_table a
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
 drop table if exists v_temp_table;
