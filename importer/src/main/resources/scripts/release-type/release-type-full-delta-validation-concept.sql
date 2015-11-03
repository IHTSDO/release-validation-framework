
/*  
	The current full concept file consists of the previously published full file and the changes for the current release
*/

	insert into qa_result (runid, assertionuuid, concept_id, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		a.id,
		concat('CONCEPT: id=',a.id, ' is in current full file, but not in prior full or current delta file.') 	
	from curr_concept_f a
	left join curr_concept_d b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.definitionstatusid = b.definitionstatusid
	left join prev_concept_f c
		on c.id = a.id
		and c.effectivetime = a.effectivetime
		and c.active = a.active
		and c.moduleid = a.moduleid
		and c.definitionstatusid = a.definitionstatusid
	where 
		( b.id is null
		or b.effectivetime is null
		or b.active is null
		or b.moduleid is null
		or b.definitionstatusid is null)
		and ( c.id is null
		or c.effectivetime is null
		or c.active is null
		or c.moduleid is null
		or c.definitionstatusid is null);
	commit;
