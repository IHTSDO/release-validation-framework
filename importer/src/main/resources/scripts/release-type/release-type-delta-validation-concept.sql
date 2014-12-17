
/*  
	The current delta file is an accurate derivative of the current full file
*/

	/* view of current delta, derived from current full */
	drop table if exists temp_view;
  create table if not exists temp_view like curr_concept_f;
  insert into temp_view
	select a.*
	from curr_concept_f a
	where a.effectivetime = '<CURRENT-RELEASE-DATE>';
	
	
/* in the delta; not in the full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept is in DELTA file, but not in FULL file.') 	
	from curr_concept_d a
	left join temp_view b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
	and a.moduleid = b.moduleid
	and a.definitionstatusid = b.definitionstatusid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.definitionstatusid is null;

/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept is in FULL file, but not in DELTA file.') 
	from temp_view a
	left join curr_concept_d b
	on a.id = b.id
	and a.effectivetime = b.effectivetime
	and a.active = b.active
	and a.moduleid = b.moduleid
	and a.definitionstatusid = b.definitionstatusid
	where b.id is null
	or b.effectivetime is null
	or b.active is null
	or b.moduleid is null
	or b.definitionstatusid is null;

	commit;
	drop table if exists temp_view;
