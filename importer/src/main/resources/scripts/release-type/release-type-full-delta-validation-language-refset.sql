
/*  
	The current full language refset file consists of the previously published full file and the changes for the current release
*/

/* view of current delta, derived from current full */
	drop temporary table if exists temp_table;
	create temporary table if not exists temp_table like prev_langrefset_f;
	
	insert into temp_table select * from curr_langrefset_d;
	commit;
	
	insert into temp_table select *	from prev_langrefset_f;
	commit;

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Language refset: id=',a.id, ': Language refset is in current full file, but not in prior full file.') 	
	from curr_langrefset_f a
	left join temp_table b 
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.acceptabilityid = b.acceptabilityid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
 	or b.refsetid is null
  	or b.referencedcomponentid is null
  	or b.acceptabilityid is null;

/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Language refset: id=',a.id, ': Language refset is in prior full file, but not in current full file.')
	from temp_table a
	left join curr_langrefset_f b 
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.acceptabilityid = b.acceptabilityid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
  	or b.refsetid is null
  	or b.referencedcomponentid is null
  	or b.acceptabilityid is null;

	commit;
	drop temporary table if exists temp_table;
