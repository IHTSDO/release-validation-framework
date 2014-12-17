
/*  
	The current full simple refset file consists of the previously published full file and the changes for the current release
*/

drop table if exists temp_table;

/* view of current delta, derived from current full */
	create table if not exists temp_table like prev_simplerefset_f;
	
	insert into temp_table 
	select * from curr_simplerefset_d;
	
	insert into temp_table 
	select * from prev_simplerefset_f;

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Simple refset: id=',a.id, ': simple refset is in current full file, but not in prior full file.') 	
	from curr_simplerefset_f a
	left join temp_table b 
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
 	or b.refsetid is null
  	or b.referencedcomponentid is null;

/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Simple refset: id=',a.id, ': simple refset is in prior full file, but not in current full file.')
	from temp_table a
	left join curr_simplerefset_f b 
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
  	or b.refsetid is null
  	or b.referencedcomponentid is null;

commit;
drop table temp_table;
