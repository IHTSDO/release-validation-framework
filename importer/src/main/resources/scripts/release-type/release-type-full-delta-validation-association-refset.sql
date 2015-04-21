
/*  
	The current full association refset file consists of the previously published full file and the changes for the current release
*/
drop table if exists v_temp_table;


/* in the current full but not in the previous full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	'<ASSERTIONTEXT>',
	concat('Association refset: id=',a.id, ' is in the current full file, but not in the prior full file.') 	
	from curr_associationrefset_f a
	left join curr_associationrefset_d b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.targetcomponentid = b.targetcomponentid
    left join prev_associationrefset_f c
    on a.id = c.id
	and a.effectivetime = c.effectivetime
	and a.active = c.active
    and a.moduleid = c.moduleid
    and a.refsetid = c.refsetid
    and a.referencedcomponentid = c.referencedcomponentid
    and a.targetcomponentid = c.targetcomponentid
	where ( b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
  	or b.refsetid is null
  	or b.referencedcomponentid is null
  	or b.targetcomponentid is null )
  	and ( c.id is null
	or c.effectivetime is null
	or c.active is null
	or c.moduleid is null
  	or c.refsetid is null
  	or c.referencedcomponentid is null
  	or c.targetcomponentid is null );
  	commit;
  	
  	/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('ASSOCIATION REFSET: id=',a.id, ': Member is in FULL file, but not in DELTA file.') 
	from curr_associationrefset_f a
	left join prev_associationrefset_f b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
    and a.moduleid = b.moduleid
    and a.refsetid = b.refsetid
    and a.referencedcomponentid = b.referencedcomponentid
    and a.targetcomponentid = b.targetcomponentid
    left join curr_associationrefset_d c
    on a.id = c.id
	and a.effectivetime = c.effectivetime
	and a.active = c.active
    and a.moduleid = c.moduleid
    and a.refsetid = c.refsetid
    and a.referencedcomponentid = c.referencedcomponentid
    and a.targetcomponentid = c.targetcomponentid
	where ( b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
  	or b.refsetid is null
  	or b.referencedcomponentid is null
  	or b.targetcomponentid is null )
  	and ( c.id is null
	or c.effectivetime is null
	or c.active is null
	or c.moduleid is null
  	or c.refsetid is null
  	or c.referencedcomponentid is null
  	or c.targetcomponentid is null );
	commit;
	