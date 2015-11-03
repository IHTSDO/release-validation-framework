
/*  
	The current language refset snapshot file is an accurate derivative of the current full file
*/

/* view of current snapshot, derived from current full */
	drop table if exists temp_langrefset_view;
  	create table if not exists temp_langrefset_view like curr_langrefset_f;
  	insert into temp_langrefset_view
	select a.*
	from curr_langrefset_f a
	where cast(a.effectivetime as datetime) = 
		(select max(cast(z.effectivetime as datetime))
		 from curr_langrefset_f z
		 where z.id = a.id);

/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
	<RUNID>,
	'<ASSERTIONUUID>',
	a.referencedcomponentid,
	concat('LANGUAGE REFSET: id=',a.id, ' is in SNAPSHOT file, but not in FULL file.')
	from curr_langrefset_s a
	left join temp_langrefset_view b
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
	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.referencedcomponentid,
		concat('LANGUAGE REFSET: id=',a.id, ' is in FULL file, but not in SNAPSHOT file.') 
	from temp_langrefset_view a
	left join curr_langrefset_s b 
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
	drop table if exists temp_langrefset_view;






