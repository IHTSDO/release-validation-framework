
/*  
	The current full concept file consists of the previously published full file and the changes for the current release
*/

/* in the current; not in the previous full */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ' is in current full, but not in previous full.') 	
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
	
/* in the full; not in the delta */
	insert into qa_result (runid, assertionuuid, assertiontext, details)
	select 
		<RUNID>,
		'<ASSERTIONUUID>',
		'<ASSERTIONTEXT>',
		concat('CONCEPT: id=',a.id, ': Concept is in current full, but not in current delta.')
	from curr_concept_f a
	left join prev_concept_f b
		on a.id = b.id
		and a.effectivetime = b.effectivetime
		and a.active = b.active
		and a.moduleid = b.moduleid
		and a.definitionstatusid = b.definitionstatusid
	left join curr_concept_d c
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
